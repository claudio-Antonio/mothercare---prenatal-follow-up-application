package br.edu.ufmt.mothercare.auth.controller

import br.edu.ufmt.mothercare.auth.dto.CadastroRequest
import br.edu.ufmt.mothercare.auth.exception.EmailJaCadastradoException
import br.edu.ufmt.mothercare.auth.security.JwtUtil
import br.edu.ufmt.mothercare.auth.security.SecurityConfig
import br.edu.ufmt.mothercare.auth.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, JwtUtil::class)
@TestPropertySource(properties = ["mothercare.jwt.secret=${AuthControllerTest.SEGREDO}"])
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @MockBean
    private lateinit var authService: AuthService

    private fun bearer(papel: String) = "Bearer " + jwtUtil.gerarToken("usuario-1", mapOf("papel" to papel))

    @Test
    fun `deve responder 400 quando o lote contem um UUID mal formado`() {
        mockMvc.perform(
            post("/auth/usuarios/lote")
                .header("Authorization", bearer("MEDICO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ids": ["nao-e-um-uuid"]}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `lote deve ser recusado sem token de medico`() {
        mockMvc.perform(
            post("/auth/usuarios/lote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ids": []}""")
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            post("/auth/usuarios/lote")
                .header("Authorization", bearer("GESTANTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ids": []}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `deve responder 400 quando o cadastro tem email invalido e senha curta`() {
        mockMvc.perform(
            post("/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome": "Maria", "email": "invalido", "senha": "123"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `deve responder 409 quando o email ja esta cadastrado (UC01 - A1)`() {
        val request = CadastroRequest(nome = "Maria", email = "maria@email.com", senha = "senhaForte123")
        `when`(authService.registrar(request, null)).thenThrow(EmailJaCadastradoException("maria@email.com"))

        mockMvc.perform(
            post("/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome": "Maria", "email": "maria@email.com", "senha": "senhaForte123"}""")
        ).andExpect(status().isConflict)
    }

    companion object {
        const val SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes-de-tamanho"
    }
}
