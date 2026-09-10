package br.edu.ufmt.mothercare.scheduling.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

/** UC03: solicitação de agendamento de consulta pela gestante. */
data class AgendamentoRequest(
    @field:NotNull
    val gestanteId: UUID,

    @field:NotNull
    @field:Future(message = "A data da consulta deve ser futura")
    val dataHoraDesejada: LocalDateTime
)

data class AgendamentoResponse(
    val id: UUID,
    val dataHora: LocalDateTime,
    val semanaGestacionalNoAgendamento: Int,
    val periodicidadeAplicada: String,
    val totalConsultasRealizadasOuAgendadas: Long,
    val mensagem: String
)
