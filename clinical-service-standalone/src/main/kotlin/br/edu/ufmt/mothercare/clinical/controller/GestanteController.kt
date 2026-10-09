package br.edu.ufmt.mothercare.clinical.controller

import br.edu.ufmt.mothercare.clinical.dto.AtualizarGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.clinical.dto.GestanteResumoResponse
import br.edu.ufmt.mothercare.clinical.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.clinical.dto.VincularGestanteRequest
import br.edu.ufmt.mothercare.clinical.service.ControleAcessoService
import br.edu.ufmt.mothercare.clinical.service.GestanteService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/gestantes")
class GestanteController(
    private val service: GestanteService,
    private val controleAcesso: ControleAcessoService
) {

    @PostMapping
    fun cadastrar(@Valid @RequestBody request: CadastroGestanteRequest): ResponseEntity<IdadeGestacionalResponse> {
        controleAcesso.verificarProprioUsuario(request.usuarioId)
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(request))
    }

    /**
     * Cadastro clínico do usuário logado (id vindo do token). Usado pelo app
     * logo após o login: 200 → vai pra home; 404 → ainda precisa do cadastro.
     */
    @GetMapping("/me")
    fun minhaGestante(): ResponseEntity<IdadeGestacionalResponse> =
        ResponseEntity.ok(service.buscarPorUsuario(controleAcesso.usuarioAutenticado().id))

    /** Perfil: corrige DUM, peso e altura iniciais (DUM errada contamina IG, trimestre e checklist). */
    @PutMapping("/me")
    fun atualizarMinhaGestante(@Valid @RequestBody request: AtualizarGestanteRequest): ResponseEntity<IdadeGestacionalResponse> =
        ResponseEntity.ok(service.atualizar(controleAcesso.exigirGestante(), request))

    @DeleteMapping("/me/medico")
    fun desvincularMedico(): ResponseEntity<Void> {
        service.desvincularMedico(controleAcesso.exigirGestante())
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/idade-gestacional")
    fun obterIdadeGestacional(@PathVariable id: UUID): ResponseEntity<IdadeGestacionalResponse> {
        controleAcesso.verificarAcesso(id)
        return ResponseEntity.ok(service.obterIdadeGestacional(id))
    }

    /**
     * Painel do médico (UC07). Restrito a MEDICO (ver SecurityConfig).
     * O medicoId vem da identidade autenticada, não da URL.
     */
    @GetMapping("/medico/minhas-gestantes")
    fun listarMinhasGestantes(
        @RequestHeader("Authorization") authorizationHeader: String
    ): ResponseEntity<List<GestanteResumoResponse>> {
        val medicoId = controleAcesso.usuarioAutenticado().id
        return ResponseEntity.ok(service.listarPorMedico(medicoId, authorizationHeader))
    }

    /**
     * Vínculo médico↔gestante: o médico informa o código de acompanhamento
     * (gestanteId) que a gestante vê na tela inicial do app. Restrito a MEDICO.
     */
    @PostMapping("/medico/vincular")
    fun vincular(@Valid @RequestBody request: VincularGestanteRequest): ResponseEntity<Void> {
        service.vincularMedico(request.gestanteId, controleAcesso.usuarioAutenticado().id)
        return ResponseEntity.noContent().build()
    }
}
