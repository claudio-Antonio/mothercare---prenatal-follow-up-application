package br.edu.ufmt.mothercare.scheduling.service

import br.edu.ufmt.mothercare.scheduling.client.ClinicalServiceClient
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.scheduling.dto.ConsultaResponse
import br.edu.ufmt.mothercare.scheduling.dto.LembreteConsultaResponse
import br.edu.ufmt.mothercare.scheduling.entity.Consulta
import br.edu.ufmt.mothercare.scheduling.entity.StatusConsulta
import br.edu.ufmt.mothercare.scheduling.exception.AgendamentoAposDppException
import br.edu.ufmt.mothercare.scheduling.exception.ConsultaNaoCancelavelException
import br.edu.ufmt.mothercare.scheduling.exception.ConsultaNaoEncontradaException
import br.edu.ufmt.mothercare.scheduling.exception.IntervaloConsultaInvalidoException
import br.edu.ufmt.mothercare.scheduling.exception.RiscoObstetricoImediatoException
import br.edu.ufmt.mothercare.scheduling.repository.ConsultaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
 * Fluxo do agendamento (seção 8.2 do TCC), nesta ordem:
 *  1. Não agendar após a DPP (seção 8.2, item 6).
 *  2. Check-in de PA obrigatório (UC04, RF04): a PA é registrada no clinical-service;
 *     se for crítica (RN02), o agendamento é bloqueado (RF05/UC05).
 *  3. Periodicidade (RN04) e intervalo mínimo entre consultas.
 *  4. Horário livre na agenda da clínica (RF03; UC03 A1 se não houver).
 */
@Service
class AgendamentoService(
    private val repository: ConsultaRepository,
    private val clinicalServiceClient: ClinicalServiceClient,
    private val disponibilidadeService: DisponibilidadeService
) {
    companion object {
        const val SEMANA_LIMIAR_PERIODICIDADE_SEMANAL = 28
        const val INTERVALO_MINIMO_SEMANAL_DIAS = 7L
        const val INTERVALO_MINIMO_MENSAL_DIAS = 28L
        const val MINIMO_CONSULTAS_RECOMENDADAS = 6L
        const val NIVEL_RISCO_CRITICO = "CRITICO"
        const val DIAS_ANTECEDENCIA_LEMBRETE = 3L
        const val MENSAGEM_RISCO_IMEDIATO =
            "Agendamento eletivo bloqueado: a pressão arterial informada indicou " +
                "risco de Síndrome Hipertensiva/Pré-eclâmpsia. Procure atendimento hospitalar imediato."
    }

    @Transactional
    fun agendar(request: AgendamentoRequest, authorizationHeader: String): AgendamentoResponse {
        val idade = clinicalServiceClient.obterIdadeGestacional(request.gestanteId, authorizationHeader)
        idade.dataProvavelParto?.let { dpp ->
            if (request.dataHoraDesejada.toLocalDate().isAfter(dpp)) throw AgendamentoAposDppException(dpp)
        }

        val triagem = clinicalServiceClient.registrarCheckInPressao(
            request.gestanteId, request.sistolica, request.diastolica, authorizationHeader
        )
        if (triagem.nivelRisco == NIVEL_RISCO_CRITICO) throw RiscoObstetricoImediatoException(MENSAGEM_RISCO_IMEDIATO)

        val semanaAtual = idade.semanas.toInt()
        val periodicidadeSemanal = semanaAtual >= SEMANA_LIMIAR_PERIODICIDADE_SEMANAL
        val intervaloMinimoDias = if (periodicidadeSemanal) INTERVALO_MINIMO_SEMANAL_DIAS else INTERVALO_MINIMO_MENSAL_DIAS

        validarIntervalo(request, intervaloMinimoDias)
        disponibilidadeService.validarHorario(request.dataHoraDesejada)

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

    /**
     * O scheduling não conhece o dono da gestante: a posse é validada pelo
     * clinical-service (ControleAcessoService), consultado com o token do usuário.
     * 403/404 de lá chegam ao app pelo GlobalExceptionHandler.
     */
    fun listar(gestanteId: UUID, authorizationHeader: String): List<ConsultaResponse> {
        clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId, authorizationHeader)
        return repository.findByGestanteIdOrderByDataHoraDesc(gestanteId).map {
            ConsultaResponse(it.id!!, it.dataHora, it.status.name, it.semanaGestacionalNoAgendamento)
        }
    }

    /** Cancelar libera a janela de intervalo mínimo (RN04 só considera consultas AGENDADA). */
    @Transactional
    fun cancelar(consultaId: UUID, authorizationHeader: String): ConsultaResponse {
        val consulta = repository.findById(consultaId).orElseThrow { ConsultaNaoEncontradaException(consultaId) }
        clinicalServiceClient.obterIdadeGestacionalEmSemanas(consulta.gestanteId, authorizationHeader)
        if (consulta.status != StatusConsulta.AGENDADA) throw ConsultaNaoCancelavelException(consulta.status)
        consulta.status = StatusConsulta.CANCELADA
        val salva = repository.save(consulta)
        return ConsultaResponse(salva.id!!, salva.dataHora, salva.status.name, salva.semanaGestacionalNoAgendamento)
    }

    /**
     * Seção 8.3 — lembrete de próxima consulta: ativo quando o intervalo recomendado
     * (7 dias a partir da 28ª semana, 28 dias antes) está a [DIAS_ANTECEDENCIA_LEMBRETE] dias
     * de expirar ou já expirou, e a gestante não tem consulta futura agendada.
     */
    fun lembrete(gestanteId: UUID, authorizationHeader: String): LembreteConsultaResponse {
        val idade = clinicalServiceClient.obterIdadeGestacional(gestanteId, authorizationHeader)
        val hoje = LocalDate.now()
        val consultas = repository.findByGestanteIdOrderByDataHoraDesc(gestanteId)
            .filter { it.status != StatusConsulta.CANCELADA }
        val temFutura = consultas.any { it.status == StatusConsulta.AGENDADA && !it.dataHora.toLocalDate().isBefore(hoje) }
        val ultima = consultas.firstOrNull()
        if (temFutura || ultima == null) return LembreteConsultaResponse(false, null, null)

        val intervaloDias = if (idade.semanas >= SEMANA_LIMIAR_PERIODICIDADE_SEMANAL) {
            INTERVALO_MINIMO_SEMANAL_DIAS
        } else {
            INTERVALO_MINIMO_MENSAL_DIAS
        }
        val prazo = ultima.dataHora.toLocalDate().plusDays(intervaloDias)
        val expirando = !hoje.isBefore(prazo.minusDays(DIAS_ANTECEDENCIA_LEMBRETE))
        if (!expirando) return LembreteConsultaResponse(false, null, prazo)

        val mensagem = "Sua última consulta foi em ${ultima.dataHora.toLocalDate().format(FORMATO_DATA)}. " +
            "Marque a próxima consulta até ${prazo.format(FORMATO_DATA)}."
        return LembreteConsultaResponse(true, mensagem, prazo)
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

    private val FORMATO_DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
}
