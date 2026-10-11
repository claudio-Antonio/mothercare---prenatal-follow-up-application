package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

@Serializable
enum class TipoExame { HEMOGLOBINA, GLICEMIA_JEJUM }

@Serializable
data class ExameLaboratorialRequest(
    val gestanteId: String,
    val tipo: TipoExame,
    val valor: Double,
    val documentoId: String? = null
)

/** UC08 - RN03: alertaDisparado true = Hb < 11 ou Glicemia >= 92. */
@Serializable
data class ExameLaboratorialResponse(
    val id: String,
    val tipo: TipoExame,
    val valor: Double,
    val alertaDisparado: Boolean,
    val mensagem: String
)

@Serializable
data class ExameHistoricoResponse(
    val id: String,
    val tipo: TipoExame,
    val valor: Double,
    val alertaDisparado: Boolean,
    val documentoId: String?,
    val registradoEm: String // Instant ISO (UTC)
)
