package br.edu.ufmt.mothercare.clinical.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

data class UsuarioResumoDto(val id: String, val nome: String)
data class LoteIdsRequest(val ids: List<String>)

@Component
class AuthServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${mothercare.auth-service.base-url}") private val baseUrl: String
) {
    fun buscarNomes(ids: List<UUID>, authorizationHeader: String): Map<UUID, String> {
        if (ids.isEmpty()) return emptyMap()

        val headers = HttpHeaders()
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader)
        headers.contentType = org.springframework.http.MediaType.APPLICATION_JSON

        val corpo = LoteIdsRequest(ids.map { it.toString() })
        val entidade = HttpEntity(corpo, headers)

        val resposta = restTemplate.exchange(
            "$baseUrl/auth/usuarios/lote",
            HttpMethod.POST,
            entidade,
            object : ParameterizedTypeReference<List<UsuarioResumoDto>>() {}
        )

        return (resposta.body ?: emptyList()).associate { UUID.fromString(it.id) to it.nome }
    }
}