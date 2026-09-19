package br.edu.ufmt.mothercare.app.data.remote.dto

data class AgendamentoRequest(
    val gestanteId: String,
    val dataHoraDesejada: String
)

data class AgendamentoResponse(
    val id: String,
    val dataHora: String,
    val semanaGestacionalNoAgendamento: Int,
    val periodicidadeAplicada: String,
    val totalConsultasRealizadasOuAgendadas: Long,
    val mensagem: String
)

data class ErroResponse(
    val timestamp: String? = null,
    val status: Int? = null,
    val mensagem: String? = null,
    val error: String? = null
)
