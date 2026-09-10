package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.RegistroPeso
import br.edu.ufmt.mothercare.clinical.repository.RegistroPesoRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/** RN05 / UC09 - Alerta de ganho de peso > 1,5 kg/mês no 2º trimestre. */
class MonitoramentoPesoServiceTest {

    private val repository = mockk<RegistroPesoRepository>()
    private val calculadora = CalculadoraGestacionalService()
    private val service = MonitoramentoPesoService(repository, calculadora)

    private fun gestanteComSemanasNoRegistro(semanasNoUltimoRegistro: Long): Gestante {
        // DUM ajustada para que "hoje" caia exatamente na semana desejada.
        val dum = LocalDate.now().minusDays(semanasNoUltimoRegistro * 7)
        return Gestante(
            id = UUID.randomUUID(),
            usuarioId = UUID.randomUUID(),
            dataUltimaMenstruacao = dum,
            pesoInicialKg = 60.0,
            alturaMetros = 1.65
        )
    }

    @Test
    fun `deve disparar alerta quando ganho ultrapassa 1,5kg em 30 dias no 2o trimestre`() {
        val gestante = gestanteComSemanasNoRegistro(20) // dentro do 2º trimestre (14-27)
        val registros = listOf(
            RegistroPeso(gestanteId = gestante.id!!, pesoKg = 60.0, registradoEm = LocalDate.now().minusDays(30)),
            RegistroPeso(gestanteId = gestante.id!!, pesoKg = 62.0, registradoEm = LocalDate.now()) // +2kg/30dias
        )
        every { repository.findByGestanteIdOrderByRegistradoEmAsc(gestante.id!!) } returns registros

        val grafico = service.montarGrafico(gestante)

        assertTrue(grafico.alertaGanhoExcessivo)
    }

    @Test
    fun `nao deve disparar alerta quando ganho esta dentro do limite`() {
        val gestante = gestanteComSemanasNoRegistro(20)
        val registros = listOf(
            RegistroPeso(gestanteId = gestante.id!!, pesoKg = 60.0, registradoEm = LocalDate.now().minusDays(30)),
            RegistroPeso(gestanteId = gestante.id!!, pesoKg = 61.0, registradoEm = LocalDate.now()) // +1kg/30dias
        )
        every { repository.findByGestanteIdOrderByRegistradoEmAsc(gestante.id!!) } returns registros

        val grafico = service.montarGrafico(gestante)

        assertFalse(grafico.alertaGanhoExcessivo)
    }

    @Test
    fun `nao deve disparar alerta fora do 2o trimestre mesmo com ganho alto`() {
        val gestante = gestanteComSemanasNoRegistro(35) // 3º trimestre
        val registros = listOf(
            RegistroPeso(gestanteId = gestante.id!!, pesoKg = 70.0, registradoEm = LocalDate.now().minusDays(30)),
            RegistroPeso(gestanteId = gestante.id!!, pesoKg = 73.0, registradoEm = LocalDate.now())
        )
        every { repository.findByGestanteIdOrderByRegistradoEmAsc(gestante.id!!) } returns registros

        val grafico = service.montarGrafico(gestante)

        assertFalse(grafico.alertaGanhoExcessivo)
    }

    @Test
    fun `nao deve avaliar ganho com apenas um registro`() {
        val gestante = gestanteComSemanasNoRegistro(20)
        every { repository.findByGestanteIdOrderByRegistradoEmAsc(gestante.id!!) } returns listOf(
            RegistroPeso(gestanteId = gestante.id!!, pesoKg = 60.0, registradoEm = LocalDate.now())
        )

        val grafico = service.montarGrafico(gestante)

        assertFalse(grafico.alertaGanhoExcessivo)
    }
}
