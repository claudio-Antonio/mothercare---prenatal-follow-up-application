package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

/**
 * UC07 - painel do médico. [nomeGestante] vem de uma chamada do
 * clinical-service ao auth-service — pode vir null se essa chamada
 * falhar (auth-service fora do ar); nesse caso mostramos o gestanteId.
 */
@Serializable
data class GestanteResumoResponse(
    val gestanteId: String,
    val nomeGestante: String?,
    val semanas: Long,
    val ultimoNivelRiscoPA: String, // "NORMAL" | "ATENCAO" | "CRITICO"
    val possuiAlertaLaboratorial: Boolean
)
