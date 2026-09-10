package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.CheckInPressaoRequest
import br.edu.ufmt.mothercare.clinical.dto.ResultadoTriagemPressaoResponse
import br.edu.ufmt.mothercare.clinical.dto.UltimoStatusPressaoResponse
import br.edu.ufmt.mothercare.clinical.service.TriagemHipertensivaService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** UC04 - Realizar Check-in de Saúde (PA) / UC05 - Bloquear Agendamento. */
@RestController
@RequestMapping("/checkin")
class CheckInController(
    private val service: TriagemHipertensivaService
) {

    @PostMapping("/pressao")
    fun checkInPressao(@Valid @RequestBody request: CheckInPressaoRequest): ResponseEntity<ResultadoTriagemPressaoResponse> =
        ResponseEntity.ok(service.avaliar(request))

    /** Consultado pelo scheduling-service (RF04/RF05/UC03) antes de confirmar um agendamento. */
    @GetMapping("/pressao/{gestanteId}/ultimo-status")
    fun ultimoStatus(@PathVariable gestanteId: UUID): ResponseEntity<UltimoStatusPressaoResponse> =
        ResponseEntity.ok(service.obterUltimoStatus(gestanteId))
}
