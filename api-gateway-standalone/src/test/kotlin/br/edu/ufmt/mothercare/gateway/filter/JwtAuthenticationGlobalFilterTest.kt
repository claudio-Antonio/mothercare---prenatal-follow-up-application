package br.edu.ufmt.mothercare.gateway.filter

import br.edu.ufmt.mothercare.gateway.security.JwtValidator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class JwtAuthenticationGlobalFilterTest {

    private val jwtValidator = mockk<JwtValidator>()
    private val filtro = JwtAuthenticationGlobalFilter(jwtValidator)

    private fun exchangeParaRota(path: String, authHeader: String? = null): MockServerWebExchange {
        val builder = MockServerHttpRequest.get(path)
        if (authHeader != null) builder.header("Authorization", authHeader)
        return MockServerWebExchange.from(builder.build())
    }

    private fun chainQuePassou(): GatewayFilterChain {
        val chain = mockk<GatewayFilterChain>()
        every { chain.filter(any()) } returns Mono.empty()
        return chain
    }

    @Test
    fun `deve deixar passar rota publica de registro sem token`() {
        val exchange = exchangeParaRota("/api/auth/registrar")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        verify(exactly = 1) { chain.filter(any()) }
        assertNull(exchange.response.statusCode)
    }

    @Test
    fun `deve deixar passar rota publica de login sem token`() {
        val exchange = exchangeParaRota("/api/auth/login")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        verify(exactly = 1) { chain.filter(any()) }
    }

    @Test
    fun `deve bloquear rota protegida sem cabecalho Authorization`() {
        val exchange = exchangeParaRota("/api/gestantes")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `deve bloquear rota protegida com token invalido`() {
        every { jwtValidator.isValid("token-ruim") } returns false
        val exchange = exchangeParaRota("/api/gestantes", authHeader = "Bearer token-ruim")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        verify(exactly = 0) { chain.filter(any()) }
    }

    @Test
    fun `deve bloquear cabecalho Authorization mal formatado (sem Bearer)`() {
        val exchange = exchangeParaRota("/api/gestantes", authHeader = "token-sem-prefixo")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
    }

    @Test
    fun `deve deixar passar rota protegida com token valido e propagar X-Auth-User`() {
        every { jwtValidator.isValid("token-bom") } returns true
        every { jwtValidator.extractSubject("token-bom") } returns "usuario-123"
        every { jwtValidator.extractRole("token-bom") } returns "GESTANTE"
        val exchange = exchangeParaRota("/api/gestantes", authHeader = "Bearer token-bom")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        verify(exactly = 1) { chain.filter(any()) }
        assertNull(exchange.response.statusCode)
    }

    @Test
    fun `deve propagar X-Auth-User e X-Auth-Role para os microsservicos internos`() {
        every { jwtValidator.isValid("token-medico") } returns true
        every { jwtValidator.extractSubject("token-medico") } returns "usuario-medico-1"
        every { jwtValidator.extractRole("token-medico") } returns "MEDICO"

        val exchange = exchangeParaRota("/api/gestantes/medico/minhas-gestantes", authHeader = "Bearer token-medico")
        val exchangeCapturado = slot<ServerWebExchange>()
        val chain = mockk<GatewayFilterChain>()
        every { chain.filter(capture(exchangeCapturado)) } returns Mono.empty()

        filtro.filter(exchange, chain).block()

        val headersPropagados = exchangeCapturado.captured.request.headers
        assertEquals("usuario-medico-1", headersPropagados.getFirst("X-Auth-User"))
        assertEquals("MEDICO", headersPropagados.getFirst("X-Auth-Role"))
    }

    @Test
    fun `deve sobrescrever X-Auth-User e X-Auth-Role forjados pelo cliente com os valores do token`() {
        every { jwtValidator.isValid("token-gestante") } returns true
        every { jwtValidator.extractSubject("token-gestante") } returns "usuario-gestante-1"
        every { jwtValidator.extractRole("token-gestante") } returns "GESTANTE"

        val request = MockServerHttpRequest.get("/api/gestantes/medico/minhas-gestantes")
            .header("Authorization", "Bearer token-gestante")
            .header("X-Auth-User", "usuario-forjado")
            .header("X-Auth-Role", "MEDICO")
            .build()
        val exchangeCapturado = slot<ServerWebExchange>()
        val chain = mockk<GatewayFilterChain>()
        every { chain.filter(capture(exchangeCapturado)) } returns Mono.empty()

        filtro.filter(MockServerWebExchange.from(request), chain).block()

        val headersPropagados = exchangeCapturado.captured.request.headers
        assertEquals(listOf("usuario-gestante-1"), headersPropagados["X-Auth-User"])
        assertEquals(listOf("GESTANTE"), headersPropagados["X-Auth-Role"])
    }

    @Test
    fun `deve remover X-Auth-User e X-Auth-Role enviados pelo cliente em rota publica`() {
        val request = MockServerHttpRequest.post("/api/auth/login")
            .header("X-Auth-User", "usuario-forjado")
            .header("X-Auth-Role", "MEDICO")
            .build()
        val exchangeCapturado = slot<ServerWebExchange>()
        val chain = mockk<GatewayFilterChain>()
        every { chain.filter(capture(exchangeCapturado)) } returns Mono.empty()

        filtro.filter(MockServerWebExchange.from(request), chain).block()

        val headersPropagados = exchangeCapturado.captured.request.headers
        assertNull(headersPropagados.getFirst("X-Auth-User"))
        assertNull(headersPropagados.getFirst("X-Auth-Role"))
    }

    @Test
    fun `nao deve tratar como publica uma rota que apenas comeca com o caminho de login`() {
        val exchange = exchangeParaRota("/api/auth/login-administrativo")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        verify(exactly = 0) { chain.filter(any()) }
    }
}
