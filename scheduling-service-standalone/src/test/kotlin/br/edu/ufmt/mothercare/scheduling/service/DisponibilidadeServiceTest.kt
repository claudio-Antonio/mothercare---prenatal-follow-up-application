package br.edu.ufmt.mothercare.scheduling.service

import br.edu.ufmt.mothercare.scheduling.entity.Consulta
import br.edu.ufmt.mothercare.scheduling.entity.StatusConsulta
import br.edu.ufmt.mothercare.scheduling.exception.HorarioIndisponivelException
import br.edu.ufmt.mothercare.scheduling.repository.ConsultaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** RF03 — horários livres da clínica (grade, ocupados, dias úteis e orientação do A1). */
class DisponibilidadeServiceTest {

    private val repository = mockk<ConsultaRepository>()

    /** Grade pequena e previsível: 08:00, 08:30, 09:00 e 09:30 (fechamento 10:00). */
    private val service = DisponibilidadeService(
        repository = repository,
        abertura = "08:00",
        fechamento = "10:00",
        duracaoMinutos = 30,
        telefoneClinica = "(66) 3421-0000"
    )

    private fun proximoDiaUtil(): LocalDate {
        var data = LocalDate.now().plusDays(1)
        while (data.dayOfWeek == DayOfWeek.SATURDAY || data.dayOfWeek == DayOfWeek.SUNDAY) data = data.plusDays(1)
        return data
    }

    private fun semOcupados(data: LocalDate) {
        every {
            repository.findByDataHoraGreaterThanEqualAndDataHoraLessThanAndStatus(
                data.atStartOfDay(), data.plusDays(1).atStartOfDay(), StatusConsulta.AGENDADA
            )
        } returns emptyList()
    }

    @Test
    fun `lista todos os horarios da grade quando o dia esta livre`() {
        val data = proximoDiaUtil()
        semOcupados(data)

        val resposta = service.consultar(data)

        assertEquals(listOf("08:00", "08:30", "09:00", "09:30"), resposta.horarios)
        assertEquals(false, resposta.semHorarios)
    }

    @Test
    fun `remove os horarios ja ocupados por consultas agendadas`() {
        val data = proximoDiaUtil()
        every {
            repository.findByDataHoraGreaterThanEqualAndDataHoraLessThanAndStatus(any(), any(), StatusConsulta.AGENDADA)
        } returns listOf(
            Consulta(id = UUID.randomUUID(), gestanteId = UUID.randomUUID(), dataHora = data.atTime(9, 0), semanaGestacionalNoAgendamento = 20)
        )

        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 30)), service.horariosLivres(data))
    }

    @Test
    fun `fim de semana nao tem horarios e orienta o contato telefonico (UC03 A1)`() {
        var sabado = LocalDate.now().plusDays(1)
        while (sabado.dayOfWeek != DayOfWeek.SATURDAY) sabado = sabado.plusDays(1)

        val resposta = service.consultar(sabado)

        assertTrue(resposta.semHorarios)
        assertTrue(resposta.mensagem!!.contains("(66) 3421-0000"))
    }

    @Test
    fun `horario ocupado ou fora da grade e recusado`() {
        val data = proximoDiaUtil()
        every {
            repository.findByDataHoraGreaterThanEqualAndDataHoraLessThanAndStatus(any(), any(), StatusConsulta.AGENDADA)
        } returns listOf(
            Consulta(id = UUID.randomUUID(), gestanteId = UUID.randomUUID(), dataHora = data.atTime(8, 30), semanaGestacionalNoAgendamento = 20)
        )

        assertThrows(HorarioIndisponivelException::class.java) { service.validarHorario(LocalDateTime.of(data, LocalTime.of(8, 30))) }
        assertThrows(HorarioIndisponivelException::class.java) { service.validarHorario(LocalDateTime.of(data, LocalTime.of(8, 15))) }
    }
}
