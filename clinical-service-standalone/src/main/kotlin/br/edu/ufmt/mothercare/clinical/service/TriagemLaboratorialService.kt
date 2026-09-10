package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialRequest
import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialResponse
import br.edu.ufmt.mothercare.clinical.entity.ExameLaboratorial
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * RN03 / UC07 / UC08 - Alerta Laboratorial.
 * - Anemia: Hemoglobina (Hb) < 11 g/dL.
 * - Diabetes Gestacional: Glicemia de Jejum >= 92 mg/dL.
 */
@Service
class TriagemLaboratorialService(
    private val repository: ExameLaboratorialRepository
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

        return ExameLaboratorialResponse(
            id = salvo.id!!,
            tipo = salvo.tipo,
            valor = salvo.valor,
            alertaDisparado = alerta,
            mensagem = mensagemPara(request.tipo, request.valor, alerta)
        )
    }

    fun disparaAlerta(tipo: TipoExame, valor: Double): Boolean = when (tipo) {
        TipoExame.HEMOGLOBINA -> valor < LIMIAR_HEMOGLOBINA_MIN
        TipoExame.GLICEMIA_JEJUM -> valor >= LIMIAR_GLICEMIA_JEJUM_MIN
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
