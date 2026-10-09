package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.GraficoPesoResponse
import br.edu.ufmt.mothercare.clinical.dto.RegistroPesoRequest
import br.edu.ufmt.mothercare.clinical.service.ControleAcessoService
import br.edu.ufmt.mothercare.clinical.service.MonitoramentoPesoService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** UC09 - Monitorar Ganho de Peso. RN05. */
@RestController
@RequestMapping("/peso")
class PesoController(
    private val monitoramentoPesoService: MonitoramentoPesoService,
    private val controleAcesso: ControleAcessoService
) {

    @PostMapping
    fun registrar(@Valid @RequestBody request: RegistroPesoRequest): ResponseEntity<GraficoPesoResponse> {
        val gestante = controleAcesso.verificarAcesso(request.gestanteId, escrita = true)
        return ResponseEntity.ok(monitoramentoPesoService.registrar(gestante, request))
    }

    @GetMapping("/{gestanteId}/grafico")
    fun grafico(@PathVariable gestanteId: UUID): ResponseEntity<GraficoPesoResponse> {
        val gestante = controleAcesso.verificarAcesso(gestanteId)
        return ResponseEntity.ok(monitoramentoPesoService.montarGrafico(gestante))
    }
}
