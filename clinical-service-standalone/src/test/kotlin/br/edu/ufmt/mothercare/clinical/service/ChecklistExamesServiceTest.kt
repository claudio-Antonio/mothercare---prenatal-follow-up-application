package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.entity.ExameLaboratorial
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import br.edu.ufmt.mothercare.clinical.repository.DocumentoClinicoRepository
import br.edu.ufmt.mothercare.clinical.repository.DocumentoResumo
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** UC06 - Visualizar Checklist de Exames, filtrado pelo trimestre atual. */
class ChecklistExamesServiceTest {

    private val exameRepository = mockk<ExameLaboratorialRepository>()
    private val calculadora = CalculadoraGestacionalService()
    private val documentoRepository = mockk<DocumentoClinicoRepository>()
    private val service = ChecklistExamesService(exameRepository, calculadora, documentoRepository)

    @BeforeEach
    fun semLaudosPorPadrao() {
        every { documentoRepository.findResumoByGestanteId(any()) } returns emptyList()
    }

    private fun laudo(categoria: String, tipo: TipoExame? = null, enviadoEm: Instant = Instant.now()) = object : DocumentoResumo {
        override val categoria = categoria
        override val tipoExameRastreado = tipo
        override val enviadoEm = enviadoEm
    }

    private fun gestanteNaSemana(semanas: Long): Gestante = Gestante(
        id = UUID.randomUUID(),
        usuarioId = UUID.randomUUID(),
        dataUltimaMenstruacao = LocalDate.now().minusDays(semanas * 7),
        pesoInicialKg = 60.0,
        alturaMetros = 1.65
    )

    @Test
    fun `deve retornar itens do 1o trimestre e marcar hemoglobina como REALIZADO se ja registrada`() {
        val gestante = gestanteNaSemana(10)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns listOf(
            ExameLaboratorial(gestanteId = gestante.id!!, tipo = TipoExame.HEMOGLOBINA, valor = 12.0, alertaDisparado = false)
        )

        val checklist = service.gerarChecklist(gestante)

        assertEquals(Trimestre.PRIMEIRO, checklist.trimestre)
        val itemHb = checklist.itens.first { it.nome.contains("Hemograma") }
        assertEquals("REALIZADO", itemHb.status)
    }

    @Test
    fun `deve marcar glicemia como PENDENTE quando ainda nao registrada`() {
        val gestante = gestanteNaSemana(10)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns emptyList()

        val checklist = service.gerarChecklist(gestante)

        val itemGlicemia = checklist.itens.first { it.nome.contains("Glicemia") }
        assertEquals("PENDENTE", itemGlicemia.status)
    }

    @Test
    fun `deve retornar itens do 2o trimestre para gestante com 20 semanas`() {
        val gestante = gestanteNaSemana(20)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns emptyList()

        val checklist = service.gerarChecklist(gestante)

        assertEquals(Trimestre.SEGUNDO, checklist.trimestre)
        assertTrue(checklist.itens.any { it.nome.contains("TOTG") })
    }

    @Test
    fun `deve retornar itens do 3o trimestre com hemograma de repeticao`() {
        val gestante = gestanteNaSemana(32)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns emptyList()

        val checklist = service.gerarChecklist(gestante)

        assertEquals(Trimestre.TERCEIRO, checklist.trimestre)
        assertEquals("PENDENTE", checklist.itens.first { it.nome.contains("Hemograma completo (repetição)") }.status)
        assertTrue(checklist.itens.any { it.nome.contains("Estreptococo") })
    }

    @Test
    fun `itens sem tipo rastreado devem ser NAO_RASTREADO`() {
        val gestante = gestanteNaSemana(10)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns emptyList()

        val checklist = service.gerarChecklist(gestante)

        val itemSorologia = checklist.itens.first { it.nome.contains("HIV") }
        assertEquals("NAO_RASTREADO", itemSorologia.status)
    }

    @Test
    fun `hemograma do 1o trimestre nao conta como a repeticao do 3o trimestre`() {
        val gestante = gestanteNaSemana(32)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns listOf(
            ExameLaboratorial(
                gestanteId = gestante.id!!, tipo = TipoExame.HEMOGLOBINA, valor = 12.0, alertaDisparado = false,
                registradoEm = Instant.now().minus(20 * 7, ChronoUnit.DAYS)
            )
        )

        val checklist = service.gerarChecklist(gestante)

        assertEquals("PENDENTE", checklist.itens.first { it.nome.contains("Hemograma completo (repetição)") }.status)
    }

    @Test
    fun `laudo enviado com o nome do item marca item nao rastreado como REALIZADO`() {
        val gestante = gestanteNaSemana(10)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns emptyList()
        every { documentoRepository.findResumoByGestanteId(gestante.id!!) } returns listOf(laudo("Sorologia para HIV"))

        val checklist = service.gerarChecklist(gestante)

        assertEquals("REALIZADO", checklist.itens.first { it.nome == "Sorologia para HIV" }.status)
        assertEquals("NAO_RASTREADO", checklist.itens.first { it.nome.contains("Sífilis") }.status)
    }

    @Test
    fun `laudo de hemoglobina conta para o item rastreado mesmo sem valor numerico`() {
        val gestante = gestanteNaSemana(10)
        every { exameRepository.findByGestanteIdOrderByRegistradoEmDesc(gestante.id!!) } returns emptyList()
        every { documentoRepository.findResumoByGestanteId(gestante.id!!) } returns
            listOf(laudo("Hemograma do posto", TipoExame.HEMOGLOBINA))

        val checklist = service.gerarChecklist(gestante)

        assertEquals("REALIZADO", checklist.itens.first { it.nome == "Hemograma completo" }.status)
    }
}
