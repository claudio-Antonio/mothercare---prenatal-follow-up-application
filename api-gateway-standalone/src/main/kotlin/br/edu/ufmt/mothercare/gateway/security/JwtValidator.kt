package br.edu.ufmt.mothercare.gateway.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class JwtValidator(
    @Value("\${mothercare.jwt.secret}") private val secret: String
) {

    private val signingKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun isValid(token: String): Boolean =
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)
            true
        } catch (ex: Exception) {
            false
        }

    fun extractSubject(token: String): String? =
        try {
            Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).payload.subject
        } catch (ex: Exception) {
            null
        }

    /** Extrai a claim "papel" (GESTANTE|MEDICO), propagada aos microsserviços via X-Auth-Role. */
    fun extractRole(token: String): String? =
        try {
            Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).payload["papel"] as? String
        } catch (ex: Exception) {
            null
        }
}
