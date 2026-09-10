package br.edu.ufmt.mothercare.scheduling.controller

import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.scheduling.service.AgendamentoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestHeader

/** UC03 - Agendar Consulta. */
@RestController
@RequestMapping("/consultas")
class AgendamentoController(
    private val service: AgendamentoService
) {

    @PostMapping
    fun agendar(
        @Valid @RequestBody request: AgendamentoRequest,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<AgendamentoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.agendar(request, authorizationHeader))
}
