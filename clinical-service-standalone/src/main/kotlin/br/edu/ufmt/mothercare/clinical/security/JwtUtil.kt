package br.edu.ufmt.mothercare.clinical.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Valida tokens JWT emitidos pelo auth-service. Compartilha o mesmo
 * segredo (`mothercare.jwt.secret`) configurado no Gateway e no
 * auth-service — não há um servidor de autenticação centralizado
 * consultado a cada requisição (evita acoplamento síncrono extra
 * entre microsserviços).
 */
@Component
class JwtUtil(
    @Value("\${mothercare.jwt.secret}") private val secret: String
) {
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun validarEExtrairSubject(token: String): String? =
        try {
            Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).payload.subject
        } catch (ex: Exception) {
            null
        }
}
