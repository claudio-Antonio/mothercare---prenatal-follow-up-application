package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import br.edu.ufmt.mothercare.clinical.entity.RegistroPressaoArterial
import br.edu.ufmt.mothercare.clinical.repository.RegistroPressaoArterialRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

/** RF04/RF05/UC03 - consulta usada pelo scheduling-service antes de confirmar agendamento. */
class TriagemHipertensivaServiceUltimoStatusTest {

    private val repository = mockk<RegistroPressaoArterialRepository>()
    private val service = TriagemHipertensivaService(repository)
    private val gestanteId = UUID.randomUUID()

    @Test
    fun `deve retornar NORMAL quando nunca houve check-in`() {
        every { repository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId) } returns emptyList()

        val status = service.obterUltimoStatus(gestanteId)

        assertEquals(NivelRisco.NORMAL, status.nivelRisco)
    }

    @Test
    fun `deve retornar o nivel de risco do check-in mais recente`() {
        val recente = RegistroPressaoArterial(gestanteId = gestanteId, sistolica = 150, diastolica = 95, nivelRisco = NivelRisco.CRITICO)
        val antigo = RegistroPressaoArterial(gestanteId = gestanteId, sistolica = 120, diastolica = 80, nivelRisco = NivelRisco.NORMAL)
        every { repository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId) } returns listOf(recente, antigo)

        val status = service.obterUltimoStatus(gestanteId)

        assertEquals(NivelRisco.CRITICO, status.nivelRisco)
    }
}
