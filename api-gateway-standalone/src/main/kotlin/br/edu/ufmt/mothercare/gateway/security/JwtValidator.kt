package br.edu.ufmt.mothercare.gateway.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Valida a assinatura e a expiração dos tokens JWT emitidos pelo
 * auth-service. O Gateway não emite tokens — só valida — usando o
 * mesmo segredo compartilhado (`mothercare.jwt.secret`) configurado em
 * todos os microsserviços. Não há chamada de rede ao auth-service a
 * cada requisição: a validação é puramente criptográfica local.
 */
@Component
class JwtValidator(
    @Value("\${mothercare.jwt.secret}") private val secret: String
) {

    private val signingKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    /** Retorna true se o token for válido (assinatura íntegra e não expirado). */
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
}
