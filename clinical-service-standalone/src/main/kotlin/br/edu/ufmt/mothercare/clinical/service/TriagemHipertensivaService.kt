package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.CheckInPressaoRequest
import br.edu.ufmt.mothercare.clinical.dto.RegistroPressaoResponse
import br.edu.ufmt.mothercare.clinical.dto.ResultadoTriagemPressaoResponse
import br.edu.ufmt.mothercare.clinical.dto.UltimoStatusPressaoResponse
import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import br.edu.ufmt.mothercare.clinical.entity.RegistroPressaoArterial
import br.edu.ufmt.mothercare.clinical.entity.TipoNotificacao
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import br.edu.ufmt.mothercare.clinical.repository.RegistroPressaoArterialRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * RN02 / UC04 / UC05 (UC11 no README) - Triagem Hipertensiva.
 * Risco obstétrico iminente quando PA Sistólica >= 140 mmHg OU
 * Diastólica >= 90 mmHg -> bloqueia o agendamento eletivo e sinaliza
 * atendimento imediato (risco de Síndrome Hipertensiva/Pré-eclâmpsia).
 *
 * Requisito RNF03 do TCC: esta triagem deve responder em menos de 1,5s —
 * a lógica é puramente aritmética (sem chamadas externas), o que a
 * mantém muito abaixo desse limite.
 */
@Service
class TriagemHipertensivaService(
    private val repository: RegistroPressaoArterialRepository,
    private val gestanteRepository: GestanteRepository,
    private val notificacaoService: NotificacaoService
) {
    companion object {
        const val LIMIAR_SISTOLICA = 140
        const val LIMIAR_DIASTOLICA = 90
    }

    @Transactional
    fun avaliar(request: CheckInPressaoRequest): ResultadoTriagemPressaoResponse {
        val risco = classificarRisco(request.sistolica, request.diastolica)

        repository.save(
            RegistroPressaoArterial(
                gestanteId = request.gestanteId,
                sistolica = request.sistolica,
                diastolica = request.diastolica,
                nivelRisco = risco
            )
        )

        return if (risco == NivelRisco.CRITICO) {
            // Seção 8.4: PA crítica notifica a gestante (tela de alerta + orientação de atendimento imediato).
            gestanteRepository.findById(request.gestanteId).ifPresent {
                notificacaoService.notificarGestante(
                    it,
                    TipoNotificacao.PRESSAO_CRITICA,
                    "Sua pressão foi ${request.sistolica}/${request.diastolica} mmHg, acima de 140/90. " +
                        "Procure atendimento hospitalar imediato. O agendamento eletivo está bloqueado."
                )
            }
            ResultadoTriagemPressaoResponse(
                nivelRisco = risco,
                agendamentoLiberado = false,
                mensagem = "Risco iminente de Síndrome Hipertensiva/Pré-eclâmpsia detectado " +
                    "(PA ${request.sistolica}/${request.diastolica} mmHg). Agendamento eletivo " +
                    "bloqueado — procure atendimento hospitalar imediato."
            )
        } else {
            ResultadoTriagemPressaoResponse(
                nivelRisco = risco,
                agendamentoLiberado = true,
                mensagem = "Pressão arterial dentro dos parâmetros esperados."
            )
        }
    }

    fun classificarRisco(sistolica: Int, diastolica: Int): NivelRisco =
        if (sistolica >= LIMIAR_SISTOLICA || diastolica >= LIMIAR_DIASTOLICA) {
            NivelRisco.CRITICO
        } else {
            NivelRisco.NORMAL
        }

    fun listarHistorico(gestanteId: UUID): List<RegistroPressaoResponse> =
        repository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId).map {
            RegistroPressaoResponse(it.id!!, it.sistolica, it.diastolica, it.nivelRisco, it.registradoEm)
        }

    /**
     * RF04/RF05/UC03 — consultado pelo scheduling-service antes de
     * confirmar um agendamento. Sem check-in prévio, retorna NORMAL
     * (não há motivo pra bloquear por ausência de dado).
     */
    fun obterUltimoStatus(gestanteId: UUID): UltimoStatusPressaoResponse {
        val ultimo = repository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId).firstOrNull()
        return UltimoStatusPressaoResponse(
            gestanteId = gestanteId,
            nivelRisco = ultimo?.nivelRisco ?: NivelRisco.NORMAL,
            registradoEm = ultimo?.registradoEm
        )
    }
}
