package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

@Serializable
data class CheckInPressaoRequest(
    val gestanteId: String,
    val sistolica: Int,
    val diastolica: Int
)

@Serializable
data class ResultadoTriagemPressaoResponse(
    val nivelRisco: String,
    val agendamentoLiberado: Boolean,
    val mensagem: String
)

/** Último check-in (RF04/RF05): CRITICO bloqueia o agendamento eletivo. registradoEm null = nunca fez check-in. */
@Serializable
data class UltimoStatusPressaoResponse(
    val gestanteId: String,
    val nivelRisco: String,
    val registradoEm: String?
)

@Serializable
data class RegistroPressaoResponse(
    val id: String,
    val sistolica: Int,
    val diastolica: Int,
    val nivelRisco: String,
    val registradoEm: String // Instant ISO (UTC)
)
