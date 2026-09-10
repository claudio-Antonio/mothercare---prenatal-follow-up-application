package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialRequest
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import br.edu.ufmt.mothercare.clinical.entity.ExameLaboratorial
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/** RN03 - Anemia: Hb < 11 g/dL. Diabetes Gestacional: Glicemia de jejum >= 92 mg/dL. */
class TriagemLaboratorialServiceTest {

    private val repository = mockk<ExameLaboratorialRepository>()
    private val service = TriagemLaboratorialService(repository)
    private val gestanteId = UUID.randomUUID()

    private fun mockSave() {
        val slot = slot<ExameLaboratorial>()
        every { repository.save(capture(slot)) } answers {
            // Sem data class, não há copy() — simula o insert reconstruindo
            // a entidade com o id gerado pelo banco (@GeneratedValue).
            ExameLaboratorial(
                id = UUID.randomUUID(),
                gestanteId = slot.captured.gestanteId,
                tipo = slot.captured.tipo,
                valor = slot.captured.valor,
                alertaDisparado = slot.captured.alertaDisparado,
                documentoId = slot.captured.documentoId
            )
        }
    }

    @Test
    fun `deve disparar alerta de anemia para Hb abaixo de 11`() {
        assertTrue(service.disparaAlerta(TipoExame.HEMOGLOBINA, 10.9))
    }

    @Test
    fun `nao deve disparar alerta de anemia para Hb igual a 11`() {
        assertFalse(service.disparaAlerta(TipoExame.HEMOGLOBINA, 11.0))
    }

    @Test
    fun `deve disparar alerta de diabetes para glicemia igual a 92 (limiar inclusivo)`() {
        assertTrue(service.disparaAlerta(TipoExame.GLICEMIA_JEJUM, 92.0))
    }

    @Test
    fun `nao deve disparar alerta de diabetes para glicemia de 91_9`() {
        assertFalse(service.disparaAlerta(TipoExame.GLICEMIA_JEJUM, 91.9))
    }

    @Test
    fun `deve retornar alertaDisparado true no response quando Hb baixa`() {
        mockSave()
        val response = service.registrar(
            ExameLaboratorialRequest(gestanteId, TipoExame.HEMOGLOBINA, 9.5)
        )
        assertTrue(response.alertaDisparado)
    }
}
