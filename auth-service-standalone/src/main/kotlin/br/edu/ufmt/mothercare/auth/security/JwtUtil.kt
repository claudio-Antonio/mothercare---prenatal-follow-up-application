package br.edu.ufmt.mothercare.auth.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

/**
 * Emissor de tokens JWT (RNF01 / RN de segurança). O mesmo segredo
 * compartilhado (`mothercare.jwt.secret`) deve ser configurado em todos
 * os microsserviços e no API Gateway para permitir a validação distribuída.
 */
@Component
class JwtUtil(
    @Value("\${mothercare.jwt.secret}") private val secret: String,
    @Value("\${mothercare.jwt.expiracao-minutos:60}") private val expiracaoMinutos: Long
) {
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    /** Valida assinatura/expiração e devolve a claim "papel"; null se o token for inválido. */
    fun extrairPapelValidado(token: String): String? =
        try {
            Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).payload["papel"] as? String
        } catch (ex: Exception) {
            null
        }

    fun gerarToken(subject: String, claims: Map<String, Any> = emptyMap()): String {
        val agora = Instant.now()
        return Jwts.builder()
            .subject(subject)
            .claims(claims)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(expiracaoMinutos * 60)))
            .signWith(signingKey)
            .compact()
    }
}
