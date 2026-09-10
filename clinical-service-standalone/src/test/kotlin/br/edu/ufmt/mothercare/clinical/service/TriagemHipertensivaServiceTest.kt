package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.CheckInPressaoRequest
import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import br.edu.ufmt.mothercare.clinical.entity.RegistroPressaoArterial
import br.edu.ufmt.mothercare.clinical.repository.RegistroPressaoArterialRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/** RN02 / UC04 / UC05 - Triagem Hipertensiva: PA Sistólica >= 140 OU Diastólica >= 90. */
class TriagemHipertensivaServiceTest {

    private val repository = mockk<RegistroPressaoArterialRepository>()
    private val service = TriagemHipertensivaService(repository)
    private val gestanteId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        // Sem "relaxed = true": o save() precisa de um retorno explícito,
        // já que RegistroPressaoArterial não tem construtor vazio e o
        // MockK não consegue gerar um valor automático pra ele.
        every { repository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `deve liberar agendamento com pressao normal`() {
        val resultado = service.avaliar(CheckInPressaoRequest(gestanteId, sistolica = 120, diastolica = 80))

        assertEquals(NivelRisco.NORMAL, resultado.nivelRisco)
        assertTrue(resultado.agendamentoLiberado)
    }

    @Test
    fun `deve bloquear agendamento quando sistolica atinge exatamente o limiar 140`() {
        val resultado = service.avaliar(CheckInPressaoRequest(gestanteId, sistolica = 140, diastolica = 80))

        assertEquals(NivelRisco.CRITICO, resultado.nivelRisco)
        assertFalse(resultado.agendamentoLiberado)
    }

    @Test
    fun `deve bloquear agendamento quando diastolica atinge exatamente o limiar 90`() {
        val resultado = service.avaliar(CheckInPressaoRequest(gestanteId, sistolica = 130, diastolica = 90))

        assertFalse(resultado.agendamentoLiberado)
    }

    @Test
    fun `nao deve bloquear com valores logo abaixo dos limiares (139-89)`() {
        val resultado = service.avaliar(CheckInPressaoRequest(gestanteId, sistolica = 139, diastolica = 89))

        assertEquals(NivelRisco.NORMAL, resultado.nivelRisco)
        assertTrue(resultado.agendamentoLiberado)
    }

    @Test
    fun `deve bloquear quando apenas a sistolica esta alterada`() {
        assertEquals(
            NivelRisco.CRITICO,
            service.classificarRisco(sistolica = 150, diastolica = 70)
        )
    }

    @Test
    fun `deve persistir o registro de pressao independente do resultado`() {
        val capturado = slot<RegistroPressaoArterial>()
        every { repository.save(capture(capturado)) } answers { capturado.captured }

        service.avaliar(CheckInPressaoRequest(gestanteId, sistolica = 160, diastolica = 100))

        assertEquals(gestanteId, capturado.captured.gestanteId)
        assertEquals(NivelRisco.CRITICO, capturado.captured.nivelRisco)
    }
}
