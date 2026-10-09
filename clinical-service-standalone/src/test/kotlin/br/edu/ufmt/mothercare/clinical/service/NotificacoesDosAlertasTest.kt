package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.CheckInPressaoRequest
import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialRequest
import br.edu.ufmt.mothercare.clinical.entity.ExameLaboratorial
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.RegistroPressaoArterial
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import br.edu.ufmt.mothercare.clinical.entity.TipoNotificacao
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import br.edu.ufmt.mothercare.clinical.repository.RegistroPressaoArterialRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

/**
 * RF08 / seção 8.4 — os alertas notificam a gestante e (quando há vínculo) o médico.
 * O limiar de Hb é exclusivo (11,0 é normal); o de glicemia é inclusivo (92 dispara).
 */
class NotificacoesDosAlertasTest {

    private val usuarioGestanteId = UUID.randomUUID()
    private val medicoId = UUID.randomUUID()
    private val gestante = Gestante(
        id = UUID.randomUUID(),
        usuarioId = usuarioGestanteId,
        dataUltimaMenstruacao = LocalDate.now().minusWeeks(20),
        pesoInicialKg = 60.0,
        alturaMetros = 1.6,
        medicoId = medicoId
    )
    private val gestanteRepository = mockk<GestanteRepository>()
    private val notificacaoService = mockk<NotificacaoService>(relaxed = true)

    init {
        every { gestanteRepository.findById(gestante.id!!) } returns Optional.of(gestante)
    }

    private fun exameService(): TriagemLaboratorialService {
        val repository = mockk<ExameLaboratorialRepository>()
        val slot = slot<ExameLaboratorial>()
        every { repository.save(capture(slot)) } answers {
            ExameLaboratorial(
                id = UUID.randomUUID(),
                gestanteId = slot.captured.gestanteId,
                tipo = slot.captured.tipo,
                valor = slot.captured.valor,
                alertaDisparado = slot.captured.alertaDisparado,
                documentoId = slot.captured.documentoId
            )
        }
        return TriagemLaboratorialService(repository, gestanteRepository, notificacaoService)
    }

    @Test
    fun `anemia notifica gestante e medico (RN03, RF08)`() {
        exameService().registrar(ExameLaboratorialRequest(gestante.id!!, TipoExame.HEMOGLOBINA, 10.9))

        verify(exactly = 1) { notificacaoService.notificarGestante(gestante, TipoNotificacao.ANEMIA, any()) }
        verify(exactly = 1) { notificacaoService.notificarMedico(gestante, TipoNotificacao.ANEMIA, any()) }
    }

    @Test
    fun `hemoglobina de 11,0 e normal e nao notifica ninguem (limiar exclusivo)`() {
        exameService().registrar(ExameLaboratorialRequest(gestante.id!!, TipoExame.HEMOGLOBINA, 11.0))

        verify(exactly = 0) { notificacaoService.notificarGestante(any(), any(), any()) }
        verify(exactly = 0) { notificacaoService.notificarMedico(any(), any(), any()) }
    }

    @Test
    fun `glicemia de 92 dispara diabetes gestacional para gestante e medico (limiar inclusivo)`() {
        exameService().registrar(ExameLaboratorialRequest(gestante.id!!, TipoExame.GLICEMIA_JEJUM, 92.0))

        verify(exactly = 1) { notificacaoService.notificarGestante(gestante, TipoNotificacao.DIABETES_GESTACIONAL, any()) }
        verify(exactly = 1) { notificacaoService.notificarMedico(gestante, TipoNotificacao.DIABETES_GESTACIONAL, any()) }
    }

    @Test
    fun `pressao critica notifica a gestante (secao 8_4)`() {
        val registros = mockk<RegistroPressaoArterialRepository>()
        every { registros.save(any<RegistroPressaoArterial>()) } answers { firstArg() }
        val triagem = TriagemHipertensivaService(registros, gestanteRepository, notificacaoService)

        triagem.avaliar(CheckInPressaoRequest(gestante.id!!, 150, 95))

        verify(exactly = 1) { notificacaoService.notificarGestante(gestante, TipoNotificacao.PRESSAO_CRITICA, any()) }
    }
}
