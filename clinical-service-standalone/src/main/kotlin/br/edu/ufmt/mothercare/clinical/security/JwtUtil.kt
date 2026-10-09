package br.edu.ufmt.mothercare.clinical.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

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

    /** Extrai a claim "papel" (GESTANTE|MEDICO) emitida pelo auth-service no login/cadastro. */
    fun extrairPapel(token: String): String? =
        try {
            Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).payload["papel"] as? String
        } catch (ex: Exception) {
            null
        }
}
