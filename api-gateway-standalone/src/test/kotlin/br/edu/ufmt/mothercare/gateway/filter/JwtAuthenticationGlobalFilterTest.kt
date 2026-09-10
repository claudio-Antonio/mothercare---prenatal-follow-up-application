package br.edu.ufmt.mothercare.gateway.filter

import br.edu.ufmt.mothercare.gateway.security.JwtValidator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono

/**
 * Testa o filtro de borda do Gateway sem subir contexto Spring nem rede
 * de verdade — só a lógica de decisão (deixar passar / bloquear / propagar
 * identidade), usando MockServerWebExchange (utilitário reativo do
 * próprio spring-test).
 */
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
        val exchange = exchangeParaRota("/api/gestantes", authHeader = "Bearer token-bom")
        val chain = chainQuePassou()

        filtro.filter(exchange, chain).block()

        verify(exactly = 1) { chain.filter(any()) }
        assertNull(exchange.response.statusCode)
    }
}
