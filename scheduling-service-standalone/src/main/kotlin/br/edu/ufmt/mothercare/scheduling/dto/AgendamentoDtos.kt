package br.edu.ufmt.mothercare.scheduling.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * UC03 + UC04: solicitação de agendamento. A PA é obrigatória (check-in de saúde
 * "invocado obrigatoriamente" pelo agendamento, RF04): sem ela não há consulta.
 * Os limites da PA são os mesmos do check-in do clinical-service.
 */
data class AgendamentoRequest(
    @field:NotNull
    val gestanteId: UUID,

    @field:NotNull
    @field:Future(message = "A data da consulta deve ser futura")
    val dataHoraDesejada: LocalDateTime,

    @field:NotNull
    @field:Min(40) @field:Max(260)
    val sistolica: Int,

    @field:NotNull
    @field:Min(20) @field:Max(200)
    val diastolica: Int
)

data class AgendamentoResponse(
    val id: UUID,
    val dataHora: LocalDateTime,
    val semanaGestacionalNoAgendamento: Int,
    val periodicidadeAplicada: String,
    val totalConsultasRealizadasOuAgendadas: Long,
    val mensagem: String
)

/** Consultas da gestante (GET /consultas?gestanteId=), da mais recente para a mais antiga. */
data class ConsultaResponse(
    val id: UUID,
    val dataHora: LocalDateTime,
    val status: String,
    val semanaGestacionalNoAgendamento: Int
)

/**
 * RF03 — horários livres da clínica numa data (GET /consultas/disponibilidade).
 * Quando não há horário, [semHorarios] = true e [mensagem] orienta a ligar para a clínica (A1 do UC03).
 */
data class DisponibilidadeResponse(
    val data: LocalDate,
    val horarios: List<String>,
    val semHorarios: Boolean,
    val mensagem: String?
)

/**
 * Seção 8.3 — lembrete de próxima consulta quando o intervalo recomendado está expirando
 * (GET /consultas/lembrete). [ativo] = false quando já há consulta futura agendada.
 */
data class LembreteConsultaResponse(
    val ativo: Boolean,
    val mensagem: String?,
    val prazoProximaConsulta: LocalDate?
)
