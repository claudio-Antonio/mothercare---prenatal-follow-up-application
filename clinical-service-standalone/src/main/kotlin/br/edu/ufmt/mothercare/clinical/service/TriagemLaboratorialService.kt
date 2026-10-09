package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.ExameHistoricoResponse
import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialRequest
import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialResponse
import br.edu.ufmt.mothercare.clinical.entity.ExameLaboratorial
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import br.edu.ufmt.mothercare.clinical.entity.TipoNotificacao
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * RN03 / UC07 / UC08 - Alerta Laboratorial.
 * - Anemia: Hemoglobina (Hb) < 11 g/dL.
 * - Diabetes Gestacional: Glicemia de Jejum >= 92 mg/dL.
 */
@Service
class TriagemLaboratorialService(
    private val repository: ExameLaboratorialRepository,
    private val gestanteRepository: GestanteRepository,
    private val notificacaoService: NotificacaoService
) {
    companion object {
        const val LIMIAR_HEMOGLOBINA_MIN = 11.0
        const val LIMIAR_GLICEMIA_JEJUM_MIN = 92.0
    }

    @Transactional
    fun registrar(request: ExameLaboratorialRequest): ExameLaboratorialResponse {
        val alerta = disparaAlerta(request.tipo, request.valor)

        val salvo = repository.save(
            ExameLaboratorial(
                gestanteId = request.gestanteId,
                tipo = request.tipo,
                valor = request.valor,
                alertaDisparado = alerta,
                documentoId = request.documentoId
            )
        )

        if (alerta) notificarAlerta(request.gestanteId, request.tipo, request.valor)

        return ExameLaboratorialResponse(
            id = salvo.id!!,
            tipo = salvo.tipo,
            valor = salvo.valor,
            alertaDisparado = alerta,
            mensagem = mensagemPara(request.tipo, request.valor, alerta)
        )
    }

    fun listarHistorico(gestanteId: UUID): List<ExameHistoricoResponse> =
        repository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId).map {
            ExameHistoricoResponse(it.id!!, it.tipo, it.valor, it.alertaDisparado, it.documentoId, it.registradoEm)
        }

    fun disparaAlerta(tipo: TipoExame, valor: Double): Boolean = when (tipo) {
        TipoExame.HEMOGLOBINA -> valor < LIMIAR_HEMOGLOBINA_MIN
        TipoExame.GLICEMIA_JEJUM -> valor >= LIMIAR_GLICEMIA_JEJUM_MIN
    }

    /**
     * RF08 / seção 8.4: anemia e glicemia notificam gestante e médico (o médico recebe
     * também a tag de atenção: possuiAlertaLaboratorial no painel). Para glicemia, a
     * gestante é orientada a agendar o TOTG (antecipação de conduta).
     */
    private fun notificarAlerta(gestanteId: UUID, tipo: TipoExame, valor: Double) {
        val gestante = gestanteRepository.findById(gestanteId).orElse(null) ?: return
        when (tipo) {
            TipoExame.HEMOGLOBINA -> {
                notificacaoService.notificarGestante(
                    gestante, TipoNotificacao.ANEMIA,
                    "Sua hemoglobina foi $valor g/dL (abaixo de $LIMIAR_HEMOGLOBINA_MIN g/dL), indicando possível anemia. Converse com seu médico."
                )
                notificacaoService.notificarMedico(
                    gestante, TipoNotificacao.ANEMIA,
                    "Alerta de anemia: hemoglobina $valor g/dL (< $LIMIAR_HEMOGLOBINA_MIN g/dL). Paciente marcada para atenção."
                )
            }
            TipoExame.GLICEMIA_JEJUM -> {
                notificacaoService.notificarGestante(
                    gestante, TipoNotificacao.DIABETES_GESTACIONAL,
                    "Sua glicemia de jejum foi $valor mg/dL (risco de diabetes gestacional). Agende o TOTG (teste oral de tolerância à glicose) com seu médico."
                )
                notificacaoService.notificarMedico(
                    gestante, TipoNotificacao.DIABETES_GESTACIONAL,
                    "Glicemia de jejum $valor mg/dL (≥ $LIMIAR_GLICEMIA_JEJUM_MIN mg/dL): risco de diabetes gestacional. Recomenda-se TOTG 75 g."
                )
            }
        }
    }

    private fun mensagemPara(tipo: TipoExame, valor: Double, alerta: Boolean): String {
        if (!alerta) return "Valor dentro da normalidade."
        return when (tipo) {
            TipoExame.HEMOGLOBINA ->
                "Alerta de possível anemia: Hb = $valor g/dL (limiar < $LIMIAR_HEMOGLOBINA_MIN g/dL)."
            TipoExame.GLICEMIA_JEJUM ->
                "Alerta de possível diabetes gestacional: Glicemia de jejum = $valor mg/dL " +
                    "(limiar >= $LIMIAR_GLICEMIA_JEJUM_MIN mg/dL)."
        }
    }
}
