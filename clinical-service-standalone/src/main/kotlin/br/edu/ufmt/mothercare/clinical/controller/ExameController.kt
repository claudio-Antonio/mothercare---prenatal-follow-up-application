package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.ChecklistExamesResponse
import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialRequest
import br.edu.ufmt.mothercare.clinical.dto.ExameLaboratorialResponse
import br.edu.ufmt.mothercare.clinical.service.ChecklistExamesService
import br.edu.ufmt.mothercare.clinical.service.GestanteService
import br.edu.ufmt.mothercare.clinical.service.TriagemLaboratorialService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** UC06 - Visualizar Checklist de Exames / UC07 - Gerenciar Prontuário / UC08 - Emitir Alertas. */
@RestController
@RequestMapping("/exames")
class ExameController(
    private val service: TriagemLaboratorialService,
    private val checklistExamesService: ChecklistExamesService,
    private val gestanteService: GestanteService
) {

    @PostMapping
    fun registrar(@Valid @RequestBody request: ExameLaboratorialRequest): ResponseEntity<ExameLaboratorialResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request))

    @GetMapping("/checklist/{gestanteId}")
    fun checklist(@PathVariable gestanteId: UUID): ResponseEntity<ChecklistExamesResponse> {
        val gestante = gestanteService.buscarOuFalhar(gestanteId)
        return ResponseEntity.ok(checklistExamesService.gerarChecklist(gestante))
    }
}
