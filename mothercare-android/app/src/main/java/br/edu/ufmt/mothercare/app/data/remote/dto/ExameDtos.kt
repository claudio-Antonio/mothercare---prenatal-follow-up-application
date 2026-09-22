package br.edu.ufmt.mothercare.app.data.remote.dto

enum class TipoExame { HEMOGLOBINA, GLICEMIA_JEJUM }

data class ExameLaboratorialRequest(
    val gestanteId: String,
    val tipo: TipoExame,
    val valor: Double,
    val documentoId: String? = null
)

/** UC08 - RN03: alertaDisparado true = Hb < 11 ou Glicemia >= 92. */
data class ExameLaboratorialResponse(
    val id: String,
    val tipo: TipoExame,
    val valor: Double,
    val alertaDisparado: Boolean,
    val mensagem: String
)
