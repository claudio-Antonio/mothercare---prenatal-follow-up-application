package br.edu.ufmt.mothercare.auth.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Duration

class JwtUtilTest {

    private val segredo = "segredo-de-teste-com-pelo-menos-32-bytes-de-tamanho"
    private val chave = Keys.hmacShaKeyFor(segredo.toByteArray(StandardCharsets.UTF_8))

    private fun lerClaims(token: String) =
        Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).payload

    @Test
    fun `deve respeitar a expiracao configurada em minutos`() {
        val token = JwtUtil(segredo, expiracaoMinutos = 60).gerarToken("usuario-123")

        val claims = lerClaims(token)
        val duracao = Duration.between(claims.issuedAt.toInstant(), claims.expiration.toInstant())
        assertEquals(Duration.ofMinutes(60), duracao)
    }

    @Test
    fun `deve incluir subject e claims customizadas no token`() {
        val token = JwtUtil(segredo, expiracaoMinutos = 5)
            .gerarToken("usuario-123", mapOf("papel" to "MEDICO"))

        val claims = lerClaims(token)
        assertEquals("usuario-123", claims.subject)
        assertEquals("MEDICO", claims["papel"])
        assertTrue(claims.expiration.after(claims.issuedAt))
    }
}
