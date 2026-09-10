package br.edu.ufmt.mothercare.scheduling.service

import br.edu.ufmt.mothercare.scheduling.client.ClinicalServiceClient
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.scheduling.entity.Consulta
import br.edu.ufmt.mothercare.scheduling.entity.StatusConsulta
import br.edu.ufmt.mothercare.scheduling.exception.IntervaloConsultaInvalidoException
import br.edu.ufmt.mothercare.scheduling.exception.RiscoObstetricoImediatoException
import br.edu.ufmt.mothercare.scheduling.repository.ConsultaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * RN04 - A partir da 28ª semana, a periodicidade de agendamento vira semanal.
 * RF04/RF05/UC03 - Agendamento bloqueado se o último check-in de PA indicar risco.
 */
class AgendamentoServiceTest {

    private val repository = mockk<ConsultaRepository>()
    private val clinicalServiceClient = mockk<ClinicalServiceClient>()
    private val service = AgendamentoService(repository, clinicalServiceClient)
    private val gestanteId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        // Padrão: nenhum risco no último check-in (a maioria dos testes
        // quer validar RN04, não a triagem de PA em si).
        every { clinicalServiceClient.obterNivelRiscoPressaoMaisRecente(gestanteId) } returns "NORMAL"
    }

    private fun mockSave() {
        val slot = slot<Consulta>()
        every { repository.save(capture(slot)) } answers {
            // Sem data class, não há copy() — simula o insert reconstruindo
            // a entidade com o id gerado pelo banco (@GeneratedValue).
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

    @Test
    fun `deve aplicar periodicidade MENSAL antes da 28a semana`() {
        every { clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId) } returns 20
        every {
            repository.existsByGestanteIdAndDataHoraBetweenAndStatus(any(), any(), any(), any())
        } returns false
        mockSave()

        val resposta = service.agendar(
            AgendamentoRequest(gestanteId, LocalDateTime.now().plusDays(10))
        )

        assertEquals("MENSAL", resposta.periodicidadeAplicada)
    }

    @Test
    fun `deve aplicar periodicidade SEMANAL a partir da 28a semana (RN04)`() {
        every { clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId) } returns 28
        every {
            repository.existsByGestanteIdAndDataHoraBetweenAndStatus(any(), any(), any(), any())
        } returns false
        mockSave()

        val resposta = service.agendar(
            AgendamentoRequest(gestanteId, LocalDateTime.now().plusDays(3))
        )

        assertEquals("SEMANAL", resposta.periodicidadeAplicada)
    }

    @Test
    fun `deve manter periodicidade MENSAL na 27a semana (limite inferior)`() {
        every { clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId) } returns 27
        every {
            repository.existsByGestanteIdAndDataHoraBetweenAndStatus(any(), any(), any(), any())
        } returns false
        mockSave()

        val resposta = service.agendar(
            AgendamentoRequest(gestanteId, LocalDateTime.now().plusDays(10))
        )

        assertEquals("MENSAL", resposta.periodicidadeAplicada)
    }

    @Test
    fun `deve rejeitar agendamento que viola o intervalo minimo semanal`() {
        every { clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId) } returns 30
        every {
            repository.existsByGestanteIdAndDataHoraBetweenAndStatus(any(), any(), any(), any())
        } returns true

        assertThrows(IntervaloConsultaInvalidoException::class.java) {
            service.agendar(AgendamentoRequest(gestanteId, LocalDateTime.now().plusDays(2)))
        }
    }

    @Test
    fun `deve bloquear agendamento quando ultimo check-in de PA indicou risco (RF05)`() {
        every { clinicalServiceClient.obterNivelRiscoPressaoMaisRecente(gestanteId) } returns "CRITICO"

        assertThrows(RiscoObstetricoImediatoException::class.java) {
            service.agendar(AgendamentoRequest(gestanteId, LocalDateTime.now().plusDays(5)))
        }

        // A triagem de risco deve ser checada ANTES de qualquer consulta à
        // idade gestacional ou persistência — não deve nem chegar lá.
        verify(exactly = 0) { clinicalServiceClient.obterIdadeGestacionalEmSemanas(any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `deve permitir agendamento quando ultimo check-in de PA esta normal`() {
        every { clinicalServiceClient.obterIdadeGestacionalEmSemanas(gestanteId) } returns 20
        every {
            repository.existsByGestanteIdAndDataHoraBetweenAndStatus(any(), any(), any(), any())
        } returns false
        mockSave()

        val resposta = service.agendar(
            AgendamentoRequest(gestanteId, LocalDateTime.now().plusDays(10))
        )

        assertEquals("MENSAL", resposta.periodicidadeAplicada)
    }
}
