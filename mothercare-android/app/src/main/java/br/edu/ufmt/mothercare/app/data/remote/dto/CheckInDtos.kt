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
