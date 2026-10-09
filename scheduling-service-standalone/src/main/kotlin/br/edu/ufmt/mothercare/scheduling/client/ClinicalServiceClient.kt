package br.edu.ufmt.mothercare.scheduling.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.UUID

/** Espelha IdadeGestacionalResponse do clinical-service (inclui a DPP usada na regra de agendamento). */
data class IdadeGestacionalDto(
    val semanas: Long,
    val dias: Long,
    val trimestre: String,
    val dataProvavelParto: LocalDate? = null
)

/** Espelha CheckInPressaoRequest do clinical-service. */
data class CheckInPressaoDto(val gestanteId: UUID, val sistolica: Int, val diastolica: Int)

/** Espelha ResultadoTriagemPressaoResponse do clinical-service (nivelRisco: "NORMAL" | "ATENCAO" | "CRITICO"). */
data class ResultadoTriagemPressaoDto(val nivelRisco: String, val agendamentoLiberado: Boolean, val mensagem: String)

/**
 * Cliente HTTP para o clinical-service. O scheduling-service não possui
 * (e não deve possuir) acesso direto ao schema `mothercare_clinical`
 * (Database per Service) — idade gestacional, DPP e check-in de PA são
 * obtidos via chamadas REST internas, mantendo o desacoplamento entre os
 * microsserviços.
 */
@Component
class ClinicalServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${mothercare.clinical-service.base-url}") private val baseUrl: String
) {
    fun obterIdadeGestacional(gestanteId: UUID, authorizationHeader: String): IdadeGestacionalDto =
        restTemplate.exchange(
            "$baseUrl/gestantes/$gestanteId/idade-gestacional",
            HttpMethod.GET,
            criarEntidadeComAuth(authorizationHeader),
            IdadeGestacionalDto::class.java
        ).body ?: error("Não foi possível obter a idade gestacional da gestante $gestanteId")

    fun obterIdadeGestacionalEmSemanas(gestanteId: UUID, authorizationHeader: String): Int =
        obterIdadeGestacional(gestanteId, authorizationHeader).semanas.toInt()

    /**
     * UC04 (check-in obrigatório do agendamento): registra a PA informada no fluxo de
     * agendamento e devolve o nível de risco (RN02). Usa o token da própria gestante.
     */
    fun registrarCheckInPressao(
        gestanteId: UUID,
        sistolica: Int,
        diastolica: Int,
        authorizationHeader: String
    ): ResultadoTriagemPressaoDto {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader)
        headers.contentType = org.springframework.http.MediaType.APPLICATION_JSON
        return restTemplate.exchange(
            "$baseUrl/checkin/pressao",
            HttpMethod.POST,
            HttpEntity(CheckInPressaoDto(gestanteId, sistolica, diastolica), headers),
            ResultadoTriagemPressaoDto::class.java
        ).body ?: error("Não foi possível registrar o check-in de pressão da gestante $gestanteId")
    }

    private fun criarEntidadeComAuth(authorizationHeader: String): HttpEntity<Void> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader)
        return HttpEntity(headers)
    }
}
