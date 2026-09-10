package br.edu.ufmt.mothercare.clinical.dto

import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.util.UUID

/** UC07/UC08: entrada de resultado laboratorial (manual ou upload). RN03. */
data class ExameLaboratorialRequest(
    @field:NotNull
    val gestanteId: UUID,

    @field:NotNull
    val tipo: TipoExame,

    @field:Positive
    val valor: Double,

    /** Preenchido quando o valor numérico foi extraído de um laudo já enviado via /prontuario/documentos. */
    val documentoId: UUID? = null
)

data class ExameLaboratorialResponse(
    val id: UUID,
    val tipo: TipoExame,
    val valor: Double,
    val alertaDisparado: Boolean,
    val mensagem: String
)
