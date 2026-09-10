package br.edu.ufmt.mothercare.scheduling.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

data class IdadeGestacionalDto(val semanas: Long, val dias: Long, val trimestre: String)

/** Espelha UltimoStatusPressaoResponse do clinical-service (nivelRisco: "NORMAL" | "CRITICO"). */
data class UltimoStatusPressaoDto(val gestanteId: String, val nivelRisco: String, val registradoEm: String?)

/**
 * Cliente HTTP para o clinical-service. O scheduling-service não possui
 * (e não deve possuir) acesso direto ao schema `mothercare_clinical`
 * (Database per Service) — idade gestacional e status de PA são obtidos
 * via chamadas REST internas, mantendo o desacoplamento entre os
 * microsserviços.
 */
@Component
class ClinicalServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${mothercare.clinical-service.base-url}") private val baseUrl: String
) {
    fun obterIdadeGestacionalEmSemanas(gestanteId: UUID, authorizationHeader: String): Int {
        val resposta = restTemplate.exchange(
            "$baseUrl/gestantes/$gestanteId/idade-gestacional",
            HttpMethod.GET,
            criarEntidadeComAuth(authorizationHeader),
            IdadeGestacionalDto::class.java
        ).body ?: error("Não foi possível obter a idade gestacional da gestante $gestanteId")
        return resposta.semanas.toInt()
    }

    /** RF04/RF05/UC03 — usado por AgendamentoService para decidir se bloqueia o agendamento. */
    fun obterNivelRiscoPressaoMaisRecente(gestanteId: UUID, authorizationHeader: String): String {
        val resposta = restTemplate.exchange(
            "$baseUrl/checkin/pressao/$gestanteId/ultimo-status",
            HttpMethod.GET,
            criarEntidadeComAuth(authorizationHeader),
            UltimoStatusPressaoDto::class.java
        ).body ?: error("Não foi possível obter o status de pressão da gestante $gestanteId")
        return resposta.nivelRisco
    }

    private fun criarEntidadeComAuth(authorizationHeader: String): HttpEntity<Void> {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader)
        return HttpEntity(headers)
    }
}