package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.RegistroPressaoResponse
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import br.edu.ufmt.mothercare.clinical.security.JwtAuthenticationFilter
import br.edu.ufmt.mothercare.clinical.security.JwtUtil
import br.edu.ufmt.mothercare.clinical.security.SecurityConfig
import br.edu.ufmt.mothercare.clinical.service.ControleAcessoService
import br.edu.ufmt.mothercare.clinical.service.TriagemHipertensivaService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

/** Médico vinculado lê o histórico de PA, mas não registra check-in em nome da gestante. */
@WebMvcTest(CheckInController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, JwtUtil::class, ControleAcessoService::class)
@TestPropertySource(properties = ["mothercare.jwt.secret=${TokensDeTeste.SEGREDO}"])
class CheckInControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: TriagemHipertensivaService

    @MockBean
    private lateinit var gestanteRepository: GestanteRepository

    private val medicoId = UUID.randomUUID()
    private val gestante = Gestante(
        id = UUID.randomUUID(), usuarioId = UUID.randomUUID(), dataUltimaMenstruacao = LocalDate.now().minusWeeks(20),
        pesoInicialKg = 60.0, alturaMetros = 1.65, medicoId = medicoId
    )

    @BeforeEach
    fun setUp() {
        `when`(gestanteRepository.findById(gestante.id!!)).thenReturn(Optional.of(gestante))
    }

    @Test
    fun `medico vinculado le o historico de PA`() {
        `when`(service.listarHistorico(gestante.id!!)).thenReturn(
            listOf(RegistroPressaoResponse(UUID.randomUUID(), 150, 95, NivelRisco.CRITICO, Instant.now()))
        )

        mockMvc.perform(
            get("/checkin/pressao/${gestante.id}").header("Authorization", TokensDeTeste.bearer(medicoId.toString(), "MEDICO"))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].nivelRisco").value("CRITICO"))
    }

    @Test
    fun `medico nao registra check-in pela gestante`() {
        mockMvc.perform(
            post("/checkin/pressao")
                .header("Authorization", TokensDeTeste.bearer(medicoId.toString(), "MEDICO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"gestanteId": "${gestante.id}", "sistolica": 120, "diastolica": 80}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `gestante registra o proprio check-in`() {
        mockMvc.perform(
            post("/checkin/pressao")
                .header("Authorization", TokensDeTeste.bearer(gestante.usuarioId.toString(), "GESTANTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"gestanteId": "${gestante.id}", "sistolica": 120, "diastolica": 80}""")
        ).andExpect(status().isOk)
    }
}
