package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.clinical.service.GestanteService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** UC01 - Manter Cadastro / UC02 - Calcular IG e DPP. */
@RestController
@RequestMapping("/gestantes")
class GestanteController(
    private val service: GestanteService
) {

    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastroGestanteRequest): ResponseEntity<IdadeGestacionalResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(request))

    @GetMapping("/{id}/idade-gestacional")
    fun obterIdadeGestacional(@PathVariable id: UUID): ResponseEntity<IdadeGestacionalResponse> =
        ResponseEntity.ok(service.obterIdadeGestacional(id))
}
