package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.entity.DocumentoClinico
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.TipoConteudoArquivo
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import br.edu.ufmt.mothercare.clinical.security.JwtAuthenticationFilter
import br.edu.ufmt.mothercare.clinical.security.JwtUtil
import br.edu.ufmt.mothercare.clinical.security.SecurityConfig
import br.edu.ufmt.mothercare.clinical.service.ControleAcessoService
import br.edu.ufmt.mothercare.clinical.service.ProntuarioDocumentoService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

/** UC07 - download do laudo com o Content-Type real do arquivo enviado. */
@WebMvcTest(ProntuarioController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, JwtUtil::class, ControleAcessoService::class)
@TestPropertySource(properties = ["mothercare.jwt.secret=${TokensDeTeste.SEGREDO}"])
class ProntuarioControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: ProntuarioDocumentoService

    @MockBean
    private lateinit var gestanteRepository: GestanteRepository

    private val usuarioId = UUID.randomUUID()
    private val token = TokensDeTeste.bearer(usuarioId.toString(), "GESTANTE")

    /** Gestante dona dos documentos — o token acima pertence a ela. */
    private val gestante = Gestante(
        id = UUID.randomUUID(), usuarioId = usuarioId, dataUltimaMenstruacao = LocalDate.now().minusWeeks(10),
        pesoInicialKg = 60.0, alturaMetros = 1.65
    )

    @BeforeEach
    fun setUp() {
        `when`(gestanteRepository.findById(gestante.id!!)).thenReturn(Optional.of(gestante))
    }

    private fun documento(nome: String, tipo: TipoConteudoArquivo, mimeType: String?) = DocumentoClinico(
        id = UUID.randomUUID(),
        gestanteId = gestante.id!!,
        categoria = "Ultrassonografia obstétrica inicial",
        nomeArquivoOriginal = nome,
        tipoConteudo = tipo,
        mimeType = mimeType,
        tamanhoBytes = 3,
        conteudo = byteArrayOf(1, 2, 3)
    )

    @Test
    fun `deve servir PNG como image png e nao como jpeg`() {
        val doc = documento("usg.png", TipoConteudoArquivo.IMAGEM, "image/png")
        `when`(service.buscarOuFalhar(doc.id!!)).thenReturn(doc)

        mockMvc.perform(get("/prontuario/documentos/conteudo/${doc.id}").header("Authorization", token))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/png"))
            .andExpect(content().bytes(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `deve codificar nome de arquivo com acento no Content-Disposition`() {
        val doc = documento("ultrassonografia_1º_trimestre.pdf", TipoConteudoArquivo.PDF, "application/pdf")
        `when`(service.buscarOuFalhar(doc.id!!)).thenReturn(doc)

        mockMvc.perform(get("/prontuario/documentos/conteudo/${doc.id}").header("Authorization", token))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''")))
    }

    @Test
    fun `nao deve servir laudo de outra gestante`() {
        val doc = documento("usg.png", TipoConteudoArquivo.IMAGEM, "image/png")
        `when`(service.buscarOuFalhar(doc.id!!)).thenReturn(doc)
        val outroToken = TokensDeTeste.bearer(UUID.randomUUID().toString(), "GESTANTE")

        mockMvc.perform(get("/prontuario/documentos/conteudo/${doc.id}").header("Authorization", outroToken))
            .andExpect(status().isForbidden)
    }
}
