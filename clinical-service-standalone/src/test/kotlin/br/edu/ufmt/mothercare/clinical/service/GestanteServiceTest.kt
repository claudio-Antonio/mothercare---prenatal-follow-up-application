package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.client.AuthServiceClient
import br.edu.ufmt.mothercare.clinical.dto.AtualizarGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.UltimoStatusPressaoResponse
import br.edu.ufmt.mothercare.clinical.entity.ExameLaboratorial
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import br.edu.ufmt.mothercare.clinical.exception.GestanteJaCadastradaException
import br.edu.ufmt.mothercare.clinical.exception.GestanteJaVinculadaException
import br.edu.ufmt.mothercare.clinical.exception.GestanteNaoEncontradaException
import br.edu.ufmt.mothercare.clinical.repository.ExameLaboratorialRepository
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class GestanteServiceTest {

    private val repository = mockk<GestanteRepository>()
    private val calculadora = CalculadoraGestacionalService()
    private val triagemHipertensivaService = mockk<TriagemHipertensivaService>()
    private val exameLaboratorialRepository = mockk<ExameLaboratorialRepository>()
    private val authServiceClient = mockk<AuthServiceClient>()

    private val service = GestanteService(
        repository, calculadora, triagemHipertensivaService, exameLaboratorialRepository, authServiceClient
    )

    private val medicoId = UUID.randomUUID()
    private val tokenFake = "Bearer token-fake"

    private fun gestanteExemplo(id: UUID = UUID.randomUUID(), usuarioId: UUID = UUID.randomUUID()): Gestante = Gestante(
        id = id,
        usuarioId = usuarioId,
        dataUltimaMenstruacao = LocalDate.now().minusWeeks(20),
        pesoInicialKg = 60.0,
        alturaMetros = 1.65,
        medicoId = medicoId
    )

    private fun cadastroRequest(usuarioId: UUID = UUID.randomUUID()) = CadastroGestanteRequest(
        usuarioId = usuarioId,
        dataUltimaMenstruacao = LocalDate.now().minusWeeks(10),
        pesoInicialKg = 60.0,
        alturaMetros = 1.65,
        medicoId = medicoId
    )

    @Test
    fun `deve cadastrar gestante e devolver idade gestacional calculada`() {
        val request = cadastroRequest()
        every { repository.existsByUsuarioId(request.usuarioId) } returns false
        every { repository.save(any()) } answers {
            val g = firstArg<Gestante>()
            Gestante(
                id = UUID.randomUUID(), usuarioId = g.usuarioId, dataUltimaMenstruacao = g.dataUltimaMenstruacao,
                pesoInicialKg = g.pesoInicialKg, alturaMetros = g.alturaMetros, medicoId = g.medicoId
            )
        }

        val response = service.cadastrar(request)

        assertEquals(10L, response.semanas)
        assertEquals(Trimestre.PRIMEIRO, response.trimestre)
    }

    @Test
    fun `nao deve cadastrar duas vezes o mesmo usuario`() {
        val request = cadastroRequest()
        every { repository.existsByUsuarioId(request.usuarioId) } returns true

        assertThrows(GestanteJaCadastradaException::class.java) { service.cadastrar(request) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `deve lancar excecao ao buscar gestante inexistente`() {
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns Optional.empty()

        assertThrows(GestanteNaoEncontradaException::class.java) { service.buscarOuFalhar(id) }
    }

    @Test
    fun `deve listar gestantes do medico com nome resolvido pelo auth-service`() {
        val gestante = gestanteExemplo()
        val gestanteId = gestante.id!!
        every { repository.findByMedicoId(medicoId) } returns listOf(gestante)
        every { triagemHipertensivaService.obterUltimoStatus(gestanteId) } returns
                UltimoStatusPressaoResponse(gestanteId, NivelRisco.NORMAL, null)
        every { exameLaboratorialRepository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId) } returns emptyList()
        every { authServiceClient.buscarNomes(listOf(gestante.usuarioId), tokenFake) } returns
                mapOf(gestante.usuarioId to "Maria Teste")

        val resumo = service.listarPorMedico(medicoId, tokenFake)

        assertEquals("Maria Teste", resumo[0].nomeGestante)
        assertEquals(NivelRisco.NORMAL, resumo[0].ultimoNivelRiscoPA)
        assertFalse(resumo[0].possuiAlertaLaboratorial)
    }

    @Test
    fun `deve marcar alerta laboratorial ativo quando ultimo exame do tipo esta fora do padrao`() {
        val gestante = gestanteExemplo()
        val gestanteId = gestante.id!!
        every { repository.findByMedicoId(medicoId) } returns listOf(gestante)
        every { triagemHipertensivaService.obterUltimoStatus(gestanteId) } returns
                UltimoStatusPressaoResponse(gestanteId, NivelRisco.NORMAL, null)
        every { exameLaboratorialRepository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId) } returns listOf(
            ExameLaboratorial(gestanteId = gestanteId, tipo = TipoExame.HEMOGLOBINA, valor = 9.5, alertaDisparado = true)
        )
        every { authServiceClient.buscarNomes(any(), tokenFake) } returns emptyMap()

        val resumo = service.listarPorMedico(medicoId, tokenFake)

        assertTrue(resumo[0].possuiAlertaLaboratorial)
    }

    @Test
    fun `nao deve marcar alerta ativo se o exame mais recente do tipo normalizou`() {
        val gestante = gestanteExemplo()
        val gestanteId = gestante.id!!
        every { repository.findByMedicoId(medicoId) } returns listOf(gestante)
        every { triagemHipertensivaService.obterUltimoStatus(gestanteId) } returns
                UltimoStatusPressaoResponse(gestanteId, NivelRisco.NORMAL, null)
        every { exameLaboratorialRepository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId) } returns listOf(
            ExameLaboratorial(gestanteId = gestanteId, tipo = TipoExame.HEMOGLOBINA, valor = 12.0, alertaDisparado = false),
            ExameLaboratorial(gestanteId = gestanteId, tipo = TipoExame.HEMOGLOBINA, valor = 9.0, alertaDisparado = true)
        )
        every { authServiceClient.buscarNomes(any(), tokenFake) } returns emptyMap()

        val resumo = service.listarPorMedico(medicoId, tokenFake)

        assertFalse(resumo[0].possuiAlertaLaboratorial)
    }

    @Test
    fun `deve retornar lista vazia quando medico nao tem gestantes vinculadas`() {
        every { repository.findByMedicoId(medicoId) } returns emptyList()

        val resumo = service.listarPorMedico(medicoId, tokenFake)

        assertTrue(resumo.isEmpty())
    }

    @Test
    fun `deve continuar funcionando sem nome quando auth-service falha`() {
        val gestante = gestanteExemplo()
        val gestanteId = gestante.id!!
        every { repository.findByMedicoId(medicoId) } returns listOf(gestante)
        every { triagemHipertensivaService.obterUltimoStatus(gestanteId) } returns
                UltimoStatusPressaoResponse(gestanteId, NivelRisco.NORMAL, null)
        every { exameLaboratorialRepository.findByGestanteIdOrderByRegistradoEmDesc(gestanteId) } returns emptyList()
        every { authServiceClient.buscarNomes(any(), tokenFake) } throws RuntimeException("auth-service indisponível")

        val resumo = service.listarPorMedico(medicoId, tokenFake)

        assertEquals(1, resumo.size)
        assertNull(resumo[0].nomeGestante)
    }

    @Test
    fun `deve encontrar o cadastro clinico pelo usuario do token`() {
        val gestante = gestanteExemplo()
        every { repository.findByUsuarioId(gestante.usuarioId) } returns gestante

        val response = service.buscarPorUsuario(gestante.usuarioId)

        assertEquals(gestante.id, response.gestanteId)
    }

    @Test
    fun `deve responder nao encontrada quando o usuario ainda nao fez o cadastro clinico`() {
        val usuarioId = UUID.randomUUID()
        every { repository.findByUsuarioId(usuarioId) } returns null

        assertThrows(GestanteNaoEncontradaException::class.java) { service.buscarPorUsuario(usuarioId) }
    }

    @Test
    fun `deve vincular medico a gestante sem medico`() {
        val gestante = Gestante(
            id = UUID.randomUUID(), usuarioId = UUID.randomUUID(), dataUltimaMenstruacao = LocalDate.now().minusWeeks(20),
            pesoInicialKg = 60.0, alturaMetros = 1.65
        )
        every { repository.findById(gestante.id!!) } returns Optional.of(gestante)
        every { repository.save(any()) } answers { firstArg() }

        service.vincularMedico(gestante.id!!, medicoId)

        assertEquals(medicoId, gestante.medicoId)
        verify { repository.save(gestante) }
    }

    @Test
    fun `nao deve trocar o medico de gestante ja vinculada a outro`() {
        val gestante = gestanteExemplo()
        every { repository.findById(gestante.id!!) } returns Optional.of(gestante)

        assertThrows(GestanteJaVinculadaException::class.java) { service.vincularMedico(gestante.id!!, UUID.randomUUID()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `gestante corrige a DUM e a idade gestacional e recalculada`() {
        val gestante = gestanteExemplo()
        every { repository.findByUsuarioId(gestante.usuarioId) } returns gestante
        every { repository.save(any()) } answers { firstArg() }

        val response = service.atualizar(
            gestante.usuarioId,
            AtualizarGestanteRequest(LocalDate.now().minusWeeks(30), pesoInicialKg = 62.0, alturaMetros = 1.60)
        )

        assertEquals(30L, response.semanas)
        assertEquals(Trimestre.TERCEIRO, response.trimestre)
        assertEquals(62.0, gestante.pesoInicialKg)
    }

    @Test
    fun `gestante desvincula o medico atual`() {
        val gestante = gestanteExemplo()
        every { repository.findByUsuarioId(gestante.usuarioId) } returns gestante
        every { repository.save(any()) } answers { firstArg() }

        service.desvincularMedico(gestante.usuarioId)

        assertNull(gestante.medicoId)
        assertFalse(service.buscarPorUsuario(gestante.usuarioId).medicoVinculado)
    }
}
