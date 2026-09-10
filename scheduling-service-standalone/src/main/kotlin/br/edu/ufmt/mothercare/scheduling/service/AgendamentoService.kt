                                                                                                                                                                                                                                        package br.edu.ufmt.mothercare.scheduling.service

import br.edu.ufmt.mothercare.scheduling.client.ClinicalServiceClient
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.scheduling.entity.Consulta
import br.edu.ufmt.mothercare.scheduling.entity.StatusConsulta
import br.edu.ufmt.mothercare.scheduling.exception.IntervaloConsultaInvalidoException
import br.edu.ufmt.mothercare.scheduling.exception.RiscoObstetricoImediatoException
import br.edu.ufmt.mothercare.scheduling.repository.ConsultaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * RN04 / UC03 - Agendar Consulta.
 * A partir da 28ª semana (início do 8º mês), o modelo de agendamento
 * passa de mensal para semanal (documento de regras de negócio e
 * Tabela 2 do TCC). O documento de regras também recomenda o mínimo de
 * 6 consultas de pré-natal ao longo da gestação — esse mínimo é
 * informativo (não bloqueia o agendamento), mas é reportado na resposta
 * para orientar a gestante.
 *
 * RF04/RF05/UC03 (passo 3, "invoca obrigatoriamente" a triagem de PA):
 * antes de confirmar, este serviço consulta o último status de pressão
 * da gestante no clinical-service. Se o último check-in indicou risco
 * (CRITICO — RN02), o agendamento é bloqueado.
 */
@Service
class AgendamentoService(
    private val repository: ConsultaRepository,
    private val clinicalServiceClient: ClinicalServiceClient
) {
    companion object {
        const val SEMANA_LIMIAR_PERIODICIDADE_SEMANAL = 28
        const val INTERVALO_MINIMO_SEMANAL_DIAS = 7L
        const val INTERVALO_MINIMO_MENSAL_DIAS = 28L
        const val MINIMO_CONSULTAS_RECOMENDADAS = 6L
        const val NIVEL_RISCO_CRITICO = "CRITICO"
    }

    @Transactional
    fun agendar(request: AgendamentoRequest, authorizationHeader: String): AgendamentoResponse {
        validarRiscoObstetrico(request.gestanteId, authorizationHeader)

        val semanaAtual = clinicalServiceClient.obterIdadeGestacionalEmSemanas(request.gestanteId, authorizationHeader)
        val periodicidadeSemanal = semanaAtual >= SEMANA_LIMIAR_PERIODICIDADE_SEMANAL
        val intervaloMinimoDias = if (periodicidadeSemanal) INTERVALO_MINIMO_SEMANAL_DIAS else INTERVALO_MINIMO_MENSAL_DIAS

        validarIntervalo(request, intervaloMinimoDias)

        val consulta = repository.save(
            Consulta(
                gestanteId = request.gestanteId,
                dataHora = request.dataHoraDesejada,
                semanaGestacionalNoAgendamento = semanaAtual
            )
        )

        val totalConsultas = repository.countByGestanteIdAndStatus(request.gestanteId, StatusConsulta.AGENDADA)

        return AgendamentoResponse(
            id = consulta.id!!,
            dataHora = consulta.dataHora,
            semanaGestacionalNoAgendamento = semanaAtual,
            periodicidadeAplicada = if (periodicidadeSemanal) "SEMANAL" else "MENSAL",
            totalConsultasRealizadasOuAgendadas = totalConsultas,
            mensagem = mensagemMinimoConsultas(totalConsultas)
        )
    }

    private fun validarRiscoObstetrico(gestanteId: UUID, authorizationHeader: String) {
        val nivelRisco = clinicalServiceClient.obterNivelRiscoPressaoMaisRecente(gestanteId, authorizationHeader)
        if (nivelRisco == NIVEL_RISCO_CRITICO) {
            throw RiscoObstetricoImediatoException(
                "Agendamento eletivo bloqueado: o último check-in de pressão arterial indicou " +
                    "risco de Síndrome Hipertensiva/Pré-eclâmpsia. Procure atendimento hospitalar imediato."
            )
        }
    }

    private fun validarIntervalo(request: AgendamentoRequest, intervaloMinimoDias: Long) {
        val inicioJanela = request.dataHoraDesejada.minusDays(intervaloMinimoDias)
        val fimJanela = request.dataHoraDesejada.plusDays(intervaloMinimoDias)

        val conflita = repository.existsByGestanteIdAndDataHoraBetweenAndStatus(
            request.gestanteId, inicioJanela, fimJanela, StatusConsulta.AGENDADA
        )

        if (conflita) {
            throw IntervaloConsultaInvalidoException(
                "Já existe uma consulta agendada dentro do intervalo mínimo de " +
                    "$intervaloMinimoDias dias exigido pela periodicidade atual."
            )
        }
    }

    private fun mensagemMinimoConsultas(total: Long): String =
        if (total < MINIMO_CONSULTAS_RECOMENDADAS) {
            "Consulta agendada. Total atual: $total de $MINIMO_CONSULTAS_RECOMENDADAS consultas mínimas recomendadas."
        } else {
            "Consulta agendada. Meta mínima de $MINIMO_CONSULTAS_RECOMENDADAS consultas já atingida."
        }
}
