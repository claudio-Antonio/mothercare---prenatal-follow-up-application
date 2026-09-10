package br.edu.ufmt.mothercare.auth.service

import br.edu.ufmt.mothercare.auth.dto.CadastroRequest
import br.edu.ufmt.mothercare.auth.dto.LoginRequest
import br.edu.ufmt.mothercare.auth.entity.Papel
import br.edu.ufmt.mothercare.auth.entity.Usuario
import br.edu.ufmt.mothercare.auth.exception.CredenciaisInvalidasException
import br.edu.ufmt.mothercare.auth.exception.EmailJaCadastradoException
import br.edu.ufmt.mothercare.auth.repository.UsuarioRepository
import br.edu.ufmt.mothercare.auth.security.JwtUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.UUID

/** Cobre UC01 (Manter Cadastro) e o fluxo alternativo A1: email já cadastrado. */
class AuthServiceTest {

    private val repository = mockk<UsuarioRepository>()
    private val jwtUtil = mockk<JwtUtil>()
    private val encoder = BCryptPasswordEncoder()
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(repository, encoder, jwtUtil)
    }

    @Test
    fun `deve registrar nova gestante e retornar token`() {
        val request = CadastroRequest(nome = "Maria Silva", email = "maria@email.com", senha = "senhaForte123")
        val savedSlot = slot<Usuario>()

        every { repository.existsByEmail(request.email) } returns false
        every { repository.save(capture(savedSlot)) } answers {
            Usuario(
                id = UUID.randomUUID(),
                nome = savedSlot.captured.nome,
                email = savedSlot.captured.email,
                senhaHash = savedSlot.captured.senhaHash,
                papel = savedSlot.captured.papel
            )
        }
        every { jwtUtil.gerarToken(any(), any()) } returns "token-fake"

        val response = authService.registrar(request)

        assertEquals("token-fake", response.token)
        assertEquals(Papel.GESTANTE, response.usuario.papel)
        // Garante que a senha nunca é persistida em texto puro.
        assertTrue(encoder.matches(request.senha, savedSlot.captured.senhaHash))
        assertNotEquals(request.senha, savedSlot.captured.senhaHash)
    }

    @Test
    fun `nao deve permitir cadastro com email ja existente (UC01 - A1)`() {
        val request = CadastroRequest(nome = "Maria Silva", email = "maria@email.com", senha = "senhaForte123")
        every { repository.existsByEmail(request.email) } returns true

        assertThrows(EmailJaCadastradoException::class.java) {
            authService.registrar(request)
        }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `deve rejeitar login com senha incorreta`() {
        val usuarioExistente = Usuario(
            id = UUID.randomUUID(),
            nome = "Maria Silva",
            email = "maria@email.com",
            senhaHash = encoder.encode("senhaCorreta123"),
            papel = Papel.GESTANTE
        )
        every { repository.findByEmail("maria@email.com") } returns usuarioExistente

        assertThrows(CredenciaisInvalidasException::class.java) {
            authService.login(LoginRequest(email = "maria@email.com", senha = "senhaErrada"))
        }
    }

    @Test
    fun `deve rejeitar login para email inexistente`() {
        every { repository.findByEmail("naoexiste@email.com") } returns null

        assertThrows(CredenciaisInvalidasException::class.java) {
            authService.login(LoginRequest(email = "naoexiste@email.com", senha = "qualquer123"))
        }
    }
}
