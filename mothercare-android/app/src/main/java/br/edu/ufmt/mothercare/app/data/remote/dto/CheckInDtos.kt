package br.edu.ufmt.mothercare.app.data.remote.dto

data class CheckInPressaoRequest(
    val gestanteId: String,
    val sistolica: Int,
    val diastolica: Int
)

data class ResultadoTriagemPressaoResponse(
    val nivelRisco: String,
    val agendamentoLiberado: Boolean,
    val mensagem: String
)

/** Último check-in (RF04/RF05): CRITICO bloqueia o agendamento eletivo. registradoEm null = nunca fez check-in. */
data class UltimoStatusPressaoResponse(
    val gestanteId: String,
    val nivelRisco: String,
    val registradoEm: String?
)

data class RegistroPressaoResponse(
    val id: String,
    val sistolica: Int,
    val diastolica: Int,
    val nivelRisco: String,
    val registradoEm: String // Instant ISO (UTC)
)
