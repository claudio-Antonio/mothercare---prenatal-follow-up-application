package br.edu.ufmt.mothercare.auth.service

import br.edu.ufmt.mothercare.auth.dto.CadastroRequest
import br.edu.ufmt.mothercare.auth.dto.LoginRequest
import br.edu.ufmt.mothercare.auth.entity.Papel
import br.edu.ufmt.mothercare.auth.entity.Usuario
import br.edu.ufmt.mothercare.auth.exception.CadastroMedicoNaoAutorizadoException
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
        authService = AuthService(repository, encoder, jwtUtil, CHAVE_MEDICO)
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

    @Test
    fun `deve autenticar login com senha correta`() {
        val id = UUID.randomUUID()
        every { repository.findByEmail("maria@email.com") } returns Usuario(
            id = id,
            nome = "Maria Silva",
            email = "maria@email.com",
            senhaHash = encoder.encode("senhaCorreta123"),
            papel = Papel.GESTANTE
        )
        every { jwtUtil.gerarToken(id.toString(), any()) } returns "token-login"

        val response = authService.login(LoginRequest(email = "maria@email.com", senha = "senhaCorreta123"))

        assertEquals("token-login", response.token)
        assertEquals(id.toString(), response.usuario.id)
    }

    /**
     * Contrato com o api-gateway e o clinical-service: ambos leem a claim
     * "papel" para autorizar rotas restritas (ex.: painel do MEDICO).
     */
    @Test
    fun `deve emitir token com subject igual ao id e claims papel e email`() {
        val request = CadastroRequest(nome = "Dr. João", email = "joao@email.com", senha = "senhaForte123", papel = Papel.MEDICO)
        val id = UUID.randomUUID()
        val claims = slot<Map<String, Any>>()
        every { repository.existsByEmail(request.email) } returns false
        every { repository.save(any()) } answers {
            val u = firstArg<Usuario>()
            Usuario(id = id, nome = u.nome, email = u.email, senhaHash = u.senhaHash, papel = u.papel)
        }
        every { jwtUtil.gerarToken(id.toString(), capture(claims)) } returns "token-medico"

        val response = authService.registrar(request, CHAVE_MEDICO)

        assertEquals(Papel.MEDICO, response.usuario.papel)
        assertEquals("MEDICO", claims.captured["papel"])
        assertEquals("joao@email.com", claims.captured["email"])
    }

    @Test
    fun `deve resolver nomes em lote omitindo ids inexistentes`() {
        val existente = UUID.randomUUID()
        val inexistente = UUID.randomUUID()
        every { repository.findAllById(listOf(existente, inexistente)) } returns listOf(
            Usuario(id = existente, nome = "Maria Silva", email = "maria@email.com", senhaHash = "x", papel = Papel.GESTANTE)
        )

        val resumo = authService.buscarResumoPorIds(listOf(existente, inexistente))

        assertEquals(1, resumo.size)
        assertEquals(existente.toString(), resumo[0].id)
        assertEquals("Maria Silva", resumo[0].nome)
    }

    @Test
    fun `nao deve criar conta MEDICO sem a chave de cadastro`() {
        val request = CadastroRequest(nome = "Falso Médico", email = "x@email.com", senha = "senhaForte123", papel = Papel.MEDICO)

        assertThrows(CadastroMedicoNaoAutorizadoException::class.java) { authService.registrar(request, null) }
        assertThrows(CadastroMedicoNaoAutorizadoException::class.java) { authService.registrar(request, "chave-errada") }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `cadastro de MEDICO fica desabilitado quando a chave nao esta configurada`() {
        val semChave = AuthService(repository, encoder, jwtUtil, "")
        val request = CadastroRequest(nome = "Dr. João", email = "joao@email.com", senha = "senhaForte123", papel = Papel.MEDICO)

        assertThrows(CadastroMedicoNaoAutorizadoException::class.java) { semChave.registrar(request, "") }
    }

    companion object {
        const val CHAVE_MEDICO = "chave-medico-teste"
    }
}
