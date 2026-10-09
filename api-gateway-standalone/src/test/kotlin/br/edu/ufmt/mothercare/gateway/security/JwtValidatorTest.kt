package br.edu.ufmt.mothercare.gateway.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

class JwtValidatorTest {

    private val segredo = "segredo-de-teste-com-pelo-menos-32-bytes-de-tamanho"
    private val validator = JwtValidator(segredo)
    private val chaveAssinatura = Keys.hmacShaKeyFor(segredo.toByteArray(StandardCharsets.UTF_8))

    private fun gerarToken(
        subject: String = "usuario-123",
        expiraEm: Instant = Instant.now().plusSeconds(3600),
        claims: Map<String, Any> = emptyMap()
    ): String =
        Jwts.builder()
            .subject(subject)
            .claims(claims)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiraEm))
            .signWith(chaveAssinatura)
            .compact()

    @Test
    fun `deve validar token assinado corretamente e nao expirado`() {
        val token = gerarToken()
        assertTrue(validator.isValid(token))
    }

    @Test
    fun `deve extrair o subject do token valido`() {
        val token = gerarToken(subject = "usuario-abc")
        assertEquals("usuario-abc", validator.extractSubject(token))
    }

    @Test
    fun `deve extrair a claim papel do token`() {
        val token = gerarToken(claims = mapOf("papel" to "MEDICO"))
        assertEquals("MEDICO", validator.extractRole(token))
    }

    @Test
    fun `extractRole deve retornar null quando o token nao tem a claim papel`() {
        val token = gerarToken()
        assertNull(validator.extractRole(token))
    }

    @Test
    fun `deve rejeitar token expirado`() {
        val tokenExpirado = gerarToken(expiraEm = Instant.now().minusSeconds(10))
        assertFalse(validator.isValid(tokenExpirado))
    }

    @Test
    fun `deve rejeitar token assinado com segredo diferente`() {
        val outraChave = Keys.hmacShaKeyFor("outro-segredo-completamente-diferente-32-bytes".toByteArray(StandardCharsets.UTF_8))
        val tokenComOutraChave = Jwts.builder()
            .subject("usuario-123")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(outraChave)
            .compact()

        assertFalse(validator.isValid(tokenComOutraChave))
    }

    @Test
    fun `deve rejeitar string que nao e um JWT`() {
        assertFalse(validator.isValid("isso-nao-e-um-token-jwt"))
    }

    @Test
    fun `extractSubject deve retornar null para token invalido`() {
        assertEquals(null, validator.extractSubject("token-invalido"))
    }
}
