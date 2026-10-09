package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.GestanteResumoResponse
import br.edu.ufmt.mothercare.clinical.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import br.edu.ufmt.mothercare.clinical.exception.GestanteJaCadastradaException
import br.edu.ufmt.mothercare.clinical.exception.GestanteNaoEncontradaException
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import br.edu.ufmt.mothercare.clinical.security.JwtAuthenticationFilter
import br.edu.ufmt.mothercare.clinical.security.JwtUtil
import br.edu.ufmt.mothercare.clinical.security.SecurityConfig
import br.edu.ufmt.mothercare.clinical.service.ControleAcessoService
import br.edu.ufmt.mothercare.clinical.service.GestanteService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

/** UC07 - painel do médico: autorização por papel e identidade vinda do token. */
@WebMvcTest(GestanteController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, JwtUtil::class, ControleAcessoService::class)
@TestPropertySource(properties = ["mothercare.jwt.secret=${TokensDeTeste.SEGREDO}"])
class GestanteControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var gestanteService: GestanteService

    @MockBean
    private lateinit var gestanteRepository: GestanteRepository

    private val medicoId = UUID.randomUUID()

    @Test
    fun `medico deve ver as proprias gestantes usando o id do token`() {
        val token = TokensDeTeste.bearer(medicoId.toString(), "MEDICO")
        val gestanteId = UUID.randomUUID()
        `when`(gestanteService.listarPorMedico(medicoId, token)).thenReturn(
            listOf(GestanteResumoResponse(gestanteId, "Maria", 20, NivelRisco.NORMAL, false))
        )

        mockMvc.perform(get("/gestantes/medico/minhas-gestantes").header("Authorization", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].gestanteId").value(gestanteId.toString()))
            .andExpect(jsonPath("$[0].nomeGestante").value("Maria"))
    }

    @Test
    fun `gestante nao pode acessar o painel do medico`() {
        val token = TokensDeTeste.bearer(UUID.randomUUID().toString(), "GESTANTE")

        mockMvc.perform(get("/gestantes/medico/minhas-gestantes").header("Authorization", token))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `headers X-Auth forjados sem token nao dao acesso ao painel do medico`() {
        mockMvc.perform(
            get("/gestantes/medico/minhas-gestantes")
                .header("X-Auth-User", medicoId.toString())
                .header("X-Auth-Role", "MEDICO")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `requisicao sem token deve ser recusada`() {
        mockMvc.perform(get("/gestantes/${UUID.randomUUID()}/idade-gestacional"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deve responder 409 ao cadastrar novamente o mesmo usuario`() {
        val usuarioId = UUID.randomUUID()
        val request = CadastroGestanteRequest(
            usuarioId = usuarioId,
            dataUltimaMenstruacao = LocalDate.of(2026, 6, 1),
            pesoInicialKg = 60.0,
            alturaMetros = 1.65
        )
        `when`(gestanteService.cadastrar(request)).thenThrow(GestanteJaCadastradaException(usuarioId))

        mockMvc.perform(
            post("/gestantes")
                .header("Authorization", TokensDeTeste.bearer(usuarioId.toString(), "GESTANTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"usuarioId": "$usuarioId", "dataUltimaMenstruacao": "2026-06-01",
                       "pesoInicialKg": 60.0, "alturaMetros": 1.65}"""
                )
        ).andExpect(status().isConflict)
    }

    private fun gestante(usuarioId: UUID, medico: UUID? = null) = Gestante(
        id = UUID.randomUUID(), usuarioId = usuarioId, dataUltimaMenstruacao = LocalDate.now().minusWeeks(20),
        pesoInicialKg = 60.0, alturaMetros = 1.65, medicoId = medico
    )

    @Test
    fun `gestante nao pode ler a idade gestacional de outra gestante`() {
        val outra = gestante(usuarioId = UUID.randomUUID())
        `when`(gestanteRepository.findById(outra.id!!)).thenReturn(Optional.of(outra))

        mockMvc.perform(
            get("/gestantes/${outra.id}/idade-gestacional")
                .header("Authorization", TokensDeTeste.bearer(UUID.randomUUID().toString(), "GESTANTE"))
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.mensagem").exists())
    }

    @Test
    fun `medico vinculado pode ler a idade gestacional da gestante`() {
        val g = gestante(usuarioId = UUID.randomUUID(), medico = medicoId)
        `when`(gestanteRepository.findById(g.id!!)).thenReturn(Optional.of(g))
        `when`(gestanteService.obterIdadeGestacional(g.id!!)).thenReturn(
            IdadeGestacionalResponse(g.id!!, g.dataUltimaMenstruacao, LocalDate.now(), 20, 0, Trimestre.SEGUNDO, 22.0)
        )

        mockMvc.perform(
            get("/gestantes/${g.id}/idade-gestacional")
                .header("Authorization", TokensDeTeste.bearer(medicoId.toString(), "MEDICO"))
        ).andExpect(status().isOk)
    }

    @Test
    fun `nao pode criar cadastro clinico para outro usuario`() {
        mockMvc.perform(
            post("/gestantes")
                .header("Authorization", TokensDeTeste.bearer(UUID.randomUUID().toString(), "GESTANTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"usuarioId": "${UUID.randomUUID()}", "dataUltimaMenstruacao": "2026-06-01",
                       "pesoInicialKg": 60.0, "alturaMetros": 1.65}"""
                )
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `me deve responder 404 quando a gestante ainda nao tem cadastro clinico`() {
        val usuarioId = UUID.randomUUID()
        `when`(gestanteService.buscarPorUsuario(usuarioId)).thenThrow(GestanteNaoEncontradaException("usuário $usuarioId"))

        mockMvc.perform(get("/gestantes/me").header("Authorization", TokensDeTeste.bearer(usuarioId.toString(), "GESTANTE")))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `medico vincula gestante pelo codigo de acompanhamento`() {
        val gestanteId = UUID.randomUUID()

        mockMvc.perform(
            post("/gestantes/medico/vincular")
                .header("Authorization", TokensDeTeste.bearer(medicoId.toString(), "MEDICO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"gestanteId": "$gestanteId"}""")
        ).andExpect(status().isNoContent)

        verify(gestanteService).vincularMedico(gestanteId, medicoId)
    }

    @Test
    fun `gestante nao pode usar o endpoint de vinculo`() {
        mockMvc.perform(
            post("/gestantes/medico/vincular")
                .header("Authorization", TokensDeTeste.bearer(UUID.randomUUID().toString(), "GESTANTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"gestanteId": "${UUID.randomUUID()}"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `data em formato errado deve voltar 400 com mensagem`() {
        val usuarioId = UUID.randomUUID()
        mockMvc.perform(
            post("/gestantes")
                .header("Authorization", TokensDeTeste.bearer(usuarioId.toString(), "GESTANTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"usuarioId": "$usuarioId", "dataUltimaMenstruacao": "01/06/2026",
                       "pesoInicialKg": 60.0, "alturaMetros": 1.65}"""
                )
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.mensagem").exists())
    }

    @Test
    fun `medico nao usa as rotas me da gestante`() {
        mockMvc.perform(
            put("/gestantes/me")
                .header("Authorization", TokensDeTeste.bearer(medicoId.toString(), "MEDICO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dataUltimaMenstruacao": "2026-06-01", "pesoInicialKg": 60.0, "alturaMetros": 1.65}""")
        ).andExpect(status().isForbidden)
    }
}
