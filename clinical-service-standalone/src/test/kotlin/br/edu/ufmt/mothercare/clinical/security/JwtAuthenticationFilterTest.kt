package br.edu.ufmt.mothercare.clinical.security

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {

    private val jwtUtil = mockk<JwtUtil>()
    private val filtro = JwtAuthenticationFilter(jwtUtil)

    @AfterEach
    fun limparContextoSeguranca() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `deve extrair subject e papel do token quando Authorization esta presente`() {
        every { jwtUtil.validarEExtrairSubject("token-bom") } returns "usuario-123"
        every { jwtUtil.extrairPapel("token-bom") } returns "MEDICO"

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer token-bom")
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        filtro.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals("usuario-123", auth.name)
        assertTrue(auth.authorities.any { it.authority == "MEDICO" })
    }

    @Test
    fun `nao deve autenticar apenas com headers X-Auth-User e X-Auth-Role (podem ser forjados)`() {
        val request = MockHttpServletRequest()
        request.addHeader("X-Auth-User", "usuario-forjado")
        request.addHeader("X-Auth-Role", "MEDICO")
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        filtro.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `deve ignorar X-Auth-Role forjado quando o token traz outro papel`() {
        every { jwtUtil.validarEExtrairSubject("token-gestante") } returns "usuario-123"
        every { jwtUtil.extrairPapel("token-gestante") } returns "GESTANTE"
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer token-gestante")
        request.addHeader("X-Auth-Role", "MEDICO")
        val chain = mockk<FilterChain>(relaxed = true)

        filtro.doFilter(request, MockHttpServletResponse(), chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertEquals(listOf("GESTANTE"), auth.authorities.map { it.authority })
    }

    @Test
    fun `nao deve autenticar com token invalido`() {
        every { jwtUtil.validarEExtrairSubject("token-ruim") } returns null
        every { jwtUtil.extrairPapel("token-ruim") } returns null
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer token-ruim")
        val chain = mockk<FilterChain>(relaxed = true)

        filtro.doFilter(request, MockHttpServletResponse(), chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `nao deve autenticar quando nao ha token nem headers propagados`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        filtro.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
