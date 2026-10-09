package br.edu.ufmt.mothercare.auth.controller

import br.edu.ufmt.mothercare.auth.dto.AuthResponse
import br.edu.ufmt.mothercare.auth.dto.CadastroRequest
import br.edu.ufmt.mothercare.auth.dto.LoginRequest
import br.edu.ufmt.mothercare.auth.dto.LoteIdsRequest
import br.edu.ufmt.mothercare.auth.dto.UsuarioResumoResponse
import br.edu.ufmt.mothercare.auth.exception.AcessoNegadoException
import br.edu.ufmt.mothercare.auth.security.JwtUtil
import br.edu.ufmt.mothercare.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

// UC01 - Manter Cadastro. Endpoints expostos apenas via API Gateway (/api/auth/**). */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtUtil: JwtUtil
) {

    @PostMapping("/registrar")
    fun registrar(
        @Valid @RequestBody request: CadastroRequest,
        @RequestHeader("X-Chave-Cadastro-Medico", required = false) chaveCadastroMedico: String?
    ): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request, chaveCadastroMedico))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    /**
     * Chamada apenas serviço-a-serviço (clinical-service → auth-service),
     * nunca diretamente pelo app. Não vai pelo api-gateway — usa a URL
     * interna do auth-service na rede do Docker. Como a porta do auth-service
     * também fica publicada no host, exige o token do MEDICO que o
     * clinical-service já propaga — sem ele, qualquer um enumeraria nomes.
     */
    @PostMapping("/usuarios/lote")
    fun buscarResumoPorIds(
        @RequestBody request: LoteIdsRequest,
        @RequestHeader("Authorization", required = false) authorization: String?
    ): ResponseEntity<List<UsuarioResumoResponse>> {
        val papel = authorization?.takeIf { it.startsWith("Bearer ") }
            ?.let { jwtUtil.extrairPapelValidado(it.removePrefix("Bearer ").trim()) }
        if (papel != "MEDICO") throw AcessoNegadoException("Consulta de usuários restrita ao painel do médico")

        val ids = request.ids.map { UUID.fromString(it) }
        return ResponseEntity.ok(authService.buscarResumoPorIds(ids))
    }
}
