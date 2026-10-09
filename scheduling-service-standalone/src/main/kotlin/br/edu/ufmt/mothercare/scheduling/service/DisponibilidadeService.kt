package br.edu.ufmt.mothercare.scheduling.service

import br.edu.ufmt.mothercare.scheduling.dto.DisponibilidadeResponse
import br.edu.ufmt.mothercare.scheduling.entity.StatusConsulta
import br.edu.ufmt.mothercare.scheduling.exception.HorarioIndisponivelException
import br.edu.ufmt.mothercare.scheduling.repository.ConsultaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * RF03 — disponibilidade de horários da clínica.
 *
 * Decisão (seção 10, item 11 do TCC não define a agenda): a clínica atende em dias úteis,
 * em uma grade de horários de [duracaoMinutos] entre [abertura] e [fechamento]. Cada
 * horário comporta uma consulta (vale para todas as gestantes). A grade é configurável
 * em `mothercare.agenda.*`.
 */
@Service
class DisponibilidadeService(
    private val repository: ConsultaRepository,
    @Value("\${mothercare.agenda.abertura:08:00}") abertura: String,
    @Value("\${mothercare.agenda.fechamento:17:00}") fechamento: String,
    @Value("\${mothercare.agenda.duracao-minutos:30}") private val duracaoMinutos: Long,
    @Value("\${mothercare.agenda.telefone-clinica:}") private val telefoneClinica: String
) {
    private val aberturaHora: LocalTime = LocalTime.parse(abertura)
    private val fechamentoHora: LocalTime = LocalTime.parse(fechamento)

    /** Horários livres da clínica numa data (vazio em fim de semana ou em data passada). */
    fun horariosLivres(data: LocalDate): List<LocalTime> {
        if (data.isBefore(LocalDate.now()) || data.dayOfWeek == DayOfWeek.SATURDAY || data.dayOfWeek == DayOfWeek.SUNDAY) {
            return emptyList()
        }
        val ocupados = repository.findByDataHoraGreaterThanEqualAndDataHoraLessThanAndStatus(
            data.atStartOfDay(), data.plusDays(1).atStartOfDay(), StatusConsulta.AGENDADA
        ).map { it.dataHora.toLocalTime().truncatedTo(ChronoUnit.MINUTES) }.toSet()

        return gradeDoDia().filter { it !in ocupados }
    }

    fun consultar(data: LocalDate): DisponibilidadeResponse {
        val horarios = horariosLivres(data)
        val semHorarios = horarios.isEmpty()
        return DisponibilidadeResponse(
            data = data,
            horarios = horarios.map { it.toString() },
            semHorarios = semHorarios,
            mensagem = if (semHorarios) mensagemSemHorario(data) else null
        )
    }

    /** Garante que o horário pedido é um horário livre da grade (senão, UC03 A1). */
    fun validarHorario(dataHora: LocalDateTime) {
        val horario = dataHora.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
        if (horario !in horariosLivres(dataHora.toLocalDate())) {
            throw HorarioIndisponivelException(mensagemSemHorario(dataHora.toLocalDate()))
        }
    }

    private fun gradeDoDia(): List<LocalTime> {
        val grade = mutableListOf<LocalTime>()
        var horario = aberturaHora
        while (!horario.plusMinutes(duracaoMinutos).isAfter(fechamentoHora)) {
            grade += horario
            horario = horario.plusMinutes(duracaoMinutos)
        }
        return grade
    }

    private fun mensagemSemHorario(data: LocalDate): String {
        val contato = if (telefoneClinica.isBlank()) "Entre em contato com a clínica" else "Ligue para a clínica: $telefoneClinica"
        return "Não há horários disponíveis em ${data}. $contato para marcar sua consulta."
    }
}
