package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.NotificacaoResponse
import br.edu.ufmt.mothercare.clinical.service.NotificacaoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** RF08 — notificações in-app de gestante e médico. */
@RestController
@RequestMapping("/notificacoes")
class NotificacaoController(
    private val service: NotificacaoService
) {

    @GetMapping
    fun listar(): ResponseEntity<List<NotificacaoResponse>> = ResponseEntity.ok(service.listarDoUsuario())

    @PatchMapping("/{id}/lida")
    fun marcarComoLida(@PathVariable id: UUID): ResponseEntity<NotificacaoResponse> =
        ResponseEntity.ok(service.marcarComoLida(id))
}
