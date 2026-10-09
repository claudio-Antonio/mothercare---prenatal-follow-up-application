package br.edu.ufmt.mothercare.clinical.controller

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

/** Emite tokens com o mesmo formato do auth-service (subject = id do usuário, claim "papel"). */
object TokensDeTeste {
    const val SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes-de-tamanho"

    private val chave = Keys.hmacShaKeyFor(SEGREDO.toByteArray(StandardCharsets.UTF_8))

    fun bearer(usuarioId: String, papel: String): String =
        "Bearer " + Jwts.builder()
            .subject(usuarioId)
            .claim("papel", papel)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(chave)
            .compact()
}
