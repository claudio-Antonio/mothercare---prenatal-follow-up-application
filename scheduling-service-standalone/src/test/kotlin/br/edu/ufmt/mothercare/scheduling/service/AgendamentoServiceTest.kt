package br.edu.ufmt.mothercare.scheduling.service

import br.edu.ufmt.mothercare.scheduling.client.ClinicalServiceClient
import br.edu.ufmt.mothercare.scheduling.client.IdadeGestacionalDto
import br.edu.ufmt.mothercare.scheduling.client.ResultadoTriagemPressaoDto
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.scheduling.entity.Consulta
import br.edu.ufmt.mothercare.scheduling.entity.StatusConsulta
import br.edu.ufmt.mothercare.scheduling.exception.AgendamentoAposDppException
import br.edu.ufmt.mothercare.scheduling.exception.ConsultaNaoCancelavelException
import br.edu.ufmt.mothercare.scheduling.exception.HorarioIndisponivelException
import br.edu.ufmt.mothercare.scheduling.exception.IntervaloConsultaInvalidoException
import br.edu.ufmt.mothercare.scheduling.exception.RiscoObstetricoImediatoException
import br.edu.ufmt.mothercare.scheduling.repository.ConsultaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

/**
 * RN04 - periodicidade mensal/semanal conforme a 28ª semana.
 * RF04/RF05/UC03/UC04 - check-in obrigatório; agendamento bloqueado se a PA for crítica.
 * RF03 - só horários livres da clínica. Seção 8.2 - não agendar após a DPP.
 */
class AgendamentoServiceTest {

    private val repository = mockk<ConsultaRepository>()
    private val clinicalServiceClient = mockk<ClinicalServiceClient>()
    private val disponibilidadeService = mockk<DisponibilidadeService>(relaxed = true)
    private val service = AgendamentoService(repository, clinicalServiceClient, disponibilidadeService)
    private val gestanteId = UUID.randomUUID()
    private val token = "Bearer token-fake"
    private val dataFutura: LocalDateTime = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0)

    @BeforeEach
    fun setUp() {
        // Padrão: gestante na 20ª semana, DPP distante, PA normal, horário livre.
        stubIdade(semanas = 20, dpp = LocalDate.now().plusMonths(3))
        stubCheckIn(nivelRisco = "NORMAL")
    }

    private fun stubIdade(semanas: Long, dpp: LocalDate?) {
        every { clinicalServiceClient.obterIdadeGestacional(gestanteId, token) } returns
            IdadeGestacionalDto(semanas = semanas, dias = 0, trimestre = "SEGUNDO", dataProvavelParto = dpp)
        every { clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId, token) } returns semanas.toInt()
    }

    private fun stubCheckIn(nivelRisco: String) {
        every { clinicalServiceClient.registrarCheckInPressao(gestanteId, 120, 80, token) } returns
            ResultadoTriagemPressaoDto(nivelRisco, nivelRisco != "CRITICO", "mensagem")
    }

    private fun requisicao(data: LocalDateTime = dataFutura, sistolica: Int = 120, diastolica: Int = 80) =
        AgendamentoRequest(gestanteId, data, sistolica, diastolica)

    private fun mockSave() {
        val slot = slot<Consulta>()
        every { repository.save(capture(slot)) } answers {
            // Sem data class, não há copy() — simula o insert reconstruindo a entidade com o id gerado.
            Consulta(
                id = UUID.randomUUID(),
                gestanteId = slot.captured.gestanteId,
                dataHora = slot.captured.dataHora,
                status = slot.captured.status,
                semanaGestacionalNoAgendamento = slot.captured.semanaGestacionalNoAgendamento
            )
        }
        every { repository.countByGestanteIdAndStatus(gestanteId, StatusConsulta.AGENDADA) } returns 1L
    }

    private fun semIntervaloConflitante() {
        every { repository.existsByGestanteIdAndDataHoraBetweenAndStatus(any(), any(), any(), any()) } returns false
    }

    @Test
    fun `deve aplicar periodicidade MENSAL antes da 28a semana`() {
        semIntervaloConflitante()
        mockSave()

        val resposta = service.agendar(requisicao(), token)

        assertEquals("MENSAL", resposta.periodicidadeAplicada)
    }

    @Test
    fun `deve aplicar periodicidade SEMANAL a partir da 28a semana (RN04)`() {
        stubIdade(semanas = 28, dpp = LocalDate.now().plusMonths(3))
        semIntervaloConflitante()
        mockSave()

        val resposta = service.agendar(requisicao(), token)

        assertEquals("SEMANAL", resposta.periodicidadeAplicada)
    }

    @Test
    fun `deve manter periodicidade MENSAL na 27a semana (limite inferior)`() {
        stubIdade(semanas = 27, dpp = LocalDate.now().plusMonths(3))
        semIntervaloConflitante()
        mockSave()

        assertEquals("MENSAL", service.agendar(requisicao(), token).periodicidadeAplicada)
    }

    @Test
    fun `deve rejeitar agendamento que viola o intervalo minimo semanal`() {
        stubIdade(semanas = 30, dpp = LocalDate.now().plusMonths(3))
        every { repository.existsByGestanteIdAndDataHoraBetweenAndStatus(any(), any(), any(), any()) } returns true

        assertThrows(IntervaloConsultaInvalidoException::class.java) {
            service.agendar(requisicao(data = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)), token)
        }
    }

    @Test
    fun `deve bloquear agendamento quando a PA informada no check-in e critica (RF05)`() {
        stubCheckIn(nivelRisco = "CRITICO")

        assertThrows(RiscoObstetricoImediatoException::class.java) { service.agendar(requisicao(), token) }

        // A triagem é feita antes de qualquer reserva: nada é persistido nem o horário é conferido.
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { disponibilidadeService.validarHorario(any()) }
    }

    @Test
    fun `deve registrar o check-in de PA com o token da gestante antes de agendar (UC04, RF04)`() {
        semIntervaloConflitante()
        mockSave()

        service.agendar(requisicao(), token)

        verify(exactly = 1) { clinicalServiceClient.registrarCheckInPressao(gestanteId, 120, 80, token) }
        verify(exactly = 1) { clinicalServiceClient.obterIdadeGestacional(gestanteId, token) }
    }

    @Test
    fun `nao deve agendar apos a DPP (secao 8_2, item 6)`() {
        val dpp = LocalDate.now().plusDays(5)
        stubIdade(semanas = 38, dpp = dpp)

        assertThrows(AgendamentoAposDppException::class.java) {
            service.agendar(requisicao(data = dpp.plusDays(1).atTime(10, 0)), token)
        }
        verify(exactly = 0) { clinicalServiceClient.registrarCheckInPressao(any(), any(), any(), any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `deve recusar horario ocupado ou fora da agenda da clinica (RF03, UC03 A1)`() {
        every { disponibilidadeService.validarHorario(dataFutura) } throws HorarioIndisponivelException("Sem horários")
        semIntervaloConflitante()

        assertThrows(HorarioIndisponivelException::class.java) { service.agendar(requisicao(), token) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `deve repassar o Authorization original ao clinical-service`() {
        semIntervaloConflitante()
        mockSave()

        service.agendar(requisicao(), token)

        // O clinical-service só autentica por JWT assinado; sem o token repassado, a
        // triagem de risco (RF05) e a validação de acesso falhariam.
        verify(atLeast = 1) { clinicalServiceClient.obterIdadeGestacional(gestanteId, token) }
        verify(atLeast = 1) { clinicalServiceClient.registrarCheckInPressao(gestanteId, 120, 80, token) }
    }

    @Test
    fun `listar valida o acesso no clinical-service antes de devolver as consultas`() {
        every { repository.findByGestanteIdOrderByDataHoraDesc(gestanteId) } returns listOf(
            Consulta(id = UUID.randomUUID(), gestanteId = gestanteId, dataHora = LocalDateTime.now().plusDays(3), semanaGestacionalNoAgendamento = 20)
        )

        val consultas = service.listar(gestanteId, token)

        assertEquals(1, consultas.size)
        assertEquals("AGENDADA", consultas[0].status)
        verify { clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId, token) }
    }

    @Test
    fun `deve cancelar consulta agendada`() {
        val consulta = Consulta(id = UUID.randomUUID(), gestanteId = gestanteId, dataHora = LocalDateTime.now().plusDays(3), semanaGestacionalNoAgendamento = 20)
        every { repository.findById(consulta.id!!) } returns Optional.of(consulta)
        every { repository.save(any()) } answers { firstArg() }

        val resposta = service.cancelar(consulta.id!!, token)

        assertEquals("CANCELADA", resposta.status)
        assertEquals(StatusConsulta.CANCELADA, consulta.status)
    }

    @Test
    fun `nao deve cancelar consulta ja cancelada`() {
        val consulta = Consulta(
            id = UUID.randomUUID(), gestanteId = gestanteId, dataHora = LocalDateTime.now().plusDays(3),
            status = StatusConsulta.CANCELADA, semanaGestacionalNoAgendamento = 20
        )
        every { repository.findById(consulta.id!!) } returns Optional.of(consulta)

        assertThrows(ConsultaNaoCancelavelException::class.java) { service.cancelar(consulta.id!!, token) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `lembrete fica ativo quando o intervalo recomendado esta expirando (secao 8_3)`() {
        val ultimaHa25Dias = Consulta(
            id = UUID.randomUUID(), gestanteId = gestanteId,
            dataHora = LocalDate.now().minusDays(25).atTime(10, 0), semanaGestacionalNoAgendamento = 20,
            status = StatusConsulta.CONCLUIDA
        )
        every { repository.findByGestanteIdOrderByDataHoraDesc(gestanteId) } returns listOf(ultimaHa25Dias)

        val lembrete = service.lembrete(gestanteId, token)

        assertTrue(lembrete.ativo)
        assertEquals(LocalDate.now().minusDays(25).plusDays(28), lembrete.prazoProximaConsulta)
    }

    @Test
    fun `lembrete fica inativo quando ja ha consulta futura agendada`() {
        val futura = Consulta(
            id = UUID.randomUUID(), gestanteId = gestanteId,
            dataHora = LocalDateTime.now().plusDays(2), semanaGestacionalNoAgendamento = 20
        )
        every { repository.findByGestanteIdOrderByDataHoraDesc(gestanteId) } returns listOf(futura)

        assertFalse(service.lembrete(gestanteId, token).ativo)
    }

    @Test
    fun `lembrete sem consultas nao gera aviso`() {
        every { repository.findByGestanteIdOrderByDataHoraDesc(gestanteId) } returns emptyList()

        val lembrete = service.lembrete(gestanteId, token)

        assertFalse(lembrete.ativo)
        assertNull(lembrete.prazoProximaConsulta)
    }
}
