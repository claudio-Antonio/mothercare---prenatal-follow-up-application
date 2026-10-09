package br.edu.ufmt.mothercare.gateway.filter

import br.edu.ufmt.mothercare.gateway.security.JwtValidator
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationGlobalFilter(
    private val jwtValidator: JwtValidator
) : GlobalFilter, Ordered {

    private val rotasPublicas = setOf(
        "/api/auth/registrar",
        "/api/auth/login"
    )

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.uri.path

        // Comparação exata: um prefixo (startsWith) deixaria passar sem
        // token qualquer caminho como "/api/auth/login-qualquer-coisa".
        if (path in rotasPublicas) {
            // A identidade só pode vir do token validado aqui — nunca de
            // headers X-Auth-* enviados pelo próprio cliente.
            val requestSemIdentidade = exchange.request.mutate()
                .headers { it.remove("X-Auth-User"); it.remove("X-Auth-Role") }
                .build()
            return chain.filter(exchange.mutate().request(requestSemIdentidade).build())
        }

        val authHeader = exchange.request.headers.getFirst("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Cabeçalho Authorization ausente ou mal formatado")
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        if (!jwtValidator.isValid(token)) {
            return unauthorized(exchange, "Token JWT inválido ou expirado")
        }

        val subject = jwtValidator.extractSubject(token) ?: ""
        val papel = jwtValidator.extractRole(token) ?: ""
        // headers.set (e não add) sobrescreve qualquer X-Auth-* forjado pelo cliente.
        val mutatedRequest = exchange.request.mutate()
            .headers {
                it.set("X-Auth-User", subject)
                it.set("X-Auth-Role", papel)
            }
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
