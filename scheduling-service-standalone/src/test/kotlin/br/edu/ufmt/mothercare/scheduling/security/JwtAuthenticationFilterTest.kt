package br.edu.ufmt.mothercare.scheduling.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.mockk.mockk
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

class JwtAuthenticationFilterTest {

    private val segredo = "segredo-de-teste-com-pelo-menos-32-bytes-de-tamanho"
    private val filtro = JwtAuthenticationFilter(JwtUtil(segredo))

    private fun token(subject: String, segredoAssinatura: String = segredo): String =
        Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(segredoAssinatura.toByteArray(StandardCharsets.UTF_8)))
            .compact()

    private fun filtrar(request: MockHttpServletRequest) =
        filtro.doFilter(request, MockHttpServletResponse(), mockk<FilterChain>(relaxed = true))

    @AfterEach
    fun limparContextoSeguranca() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `deve autenticar com token valido`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer ${token("usuario-123")}")

        filtrar(request)

        assertEquals("usuario-123", SecurityContextHolder.getContext().authentication.name)
    }

    @Test
    fun `nao deve autenticar apenas com header X-Auth-User (pode ser forjado)`() {
        val request = MockHttpServletRequest()
        request.addHeader("X-Auth-User", "usuario-forjado")

        filtrar(request)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `nao deve autenticar com token assinado por outro segredo`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer ${token("usuario-123", "outro-segredo-completamente-diferente-32-bytes")}")
        request.addHeader("X-Auth-User", "usuario-123")

        filtrar(request)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `deve propagar o papel do token como authority`() {
        val request = MockHttpServletRequest()
        val comPapel = Jwts.builder()
            .subject("usuario-123").claim("papel", "GESTANTE")
            .issuedAt(Date.from(Instant.now())).expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(Keys.hmacShaKeyFor(segredo.toByteArray(StandardCharsets.UTF_8)))
            .compact()
        request.addHeader("Authorization", "Bearer $comPapel")

        filtrar(request)

        assertEquals("GESTANTE", SecurityContextHolder.getContext().authentication.authorities.single().authority)
    }
}
