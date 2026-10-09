package br.edu.ufmt.mothercare.scheduling.controller

import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.scheduling.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.scheduling.dto.DisponibilidadeResponse
import br.edu.ufmt.mothercare.scheduling.dto.LembreteConsultaResponse
import br.edu.ufmt.mothercare.scheduling.service.AgendamentoService
import br.edu.ufmt.mothercare.scheduling.service.DisponibilidadeService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import br.edu.ufmt.mothercare.scheduling.dto.ConsultaResponse
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

/** UC03 - Agendar Consulta. */
@RestController
@RequestMapping("/consultas")
class AgendamentoController(
    private val service: AgendamentoService,
    private val disponibilidadeService: DisponibilidadeService
) {

    @PostMapping
    fun agendar(
        @Valid @RequestBody request: AgendamentoRequest,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<AgendamentoResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.agendar(request, authorizationHeader))

    @GetMapping
    fun listar(
        @RequestParam gestanteId: UUID,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<List<ConsultaResponse>> =
        ResponseEntity.ok(service.listar(gestanteId, authorizationHeader))

    /** RF03: horários livres da clínica na data informada (yyyy-MM-dd). */
    @GetMapping("/disponibilidade")
    fun disponibilidade(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) data: LocalDate
    ): ResponseEntity<DisponibilidadeResponse> = ResponseEntity.ok(disponibilidadeService.consultar(data))

    /** Seção 8.3: lembrete de próxima consulta da gestante. */
    @GetMapping("/lembrete")
    fun lembrete(
        @RequestParam gestanteId: UUID,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<LembreteConsultaResponse> =
        ResponseEntity.ok(service.lembrete(gestanteId, authorizationHeader))

    @PatchMapping("/{id}/cancelar")
    fun cancelar(
        @PathVariable id: UUID,
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<ConsultaResponse> =
        ResponseEntity.ok(service.cancelar(id, authorizationHeader))
}
