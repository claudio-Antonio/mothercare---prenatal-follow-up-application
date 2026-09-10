package br.edu.ufmt.mothercare.gateway.filter

import br.edu.ufmt.mothercare.gateway.security.JwtValidator
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Filtro global de segurança do API Gateway (Spring Cloud Gateway).
 *
 * Intercepta toda requisição recebida do app móvel e valida o cabeçalho
 * `Authorization: Bearer <token>` antes de encaminhar o tráfego para os
 * microsserviços internos, conforme a Seção 4.2.2 do TCC ("validar
 * tokens de segurança (JWT) e efetuar o roteamento de rede").
 *
 * Rotas públicas (cadastro e login) são explicitamente ignoradas — faz
 * sentido, já que é ali que o token nasce.
 */
@Component
class JwtAuthenticationGlobalFilter(
    private val jwtValidator: JwtValidator
) : GlobalFilter, Ordered {

    private val rotasPublicas = listOf(
        "/api/auth/registrar",
        "/api/auth/login"
    )

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.uri.path

        if (rotasPublicas.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = exchange.request.headers.getFirst("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Cabeçalho Authorization ausente ou mal formatado")
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        if (!jwtValidator.isValid(token)) {
            return unauthorized(exchange, "Token JWT inválido ou expirado")
        }

        // Propaga a identidade do usuário para os microsserviços internos,
        // evitando que cada serviço precise revalidar o token sozinho
        // (embora cada um também valide — defesa em profundidade).
        val subject = jwtValidator.extractSubject(token) ?: ""
        val mutatedRequest = exchange.request.mutate()
            .header("X-Auth-User", subject)
            .build()

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
    }

    private fun unauthorized(exchange: ServerWebExchange, mensagem: String): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        exchange.response.headers.add("X-Auth-Error", mensagem)
        return exchange.response.setComplete()
    }

    override fun getOrder(): Int = -1
}
