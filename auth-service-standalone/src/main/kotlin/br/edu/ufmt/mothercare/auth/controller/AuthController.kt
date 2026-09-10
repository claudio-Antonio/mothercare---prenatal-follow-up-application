package br.edu.ufmt.mothercare.auth.controller

import br.edu.ufmt.mothercare.auth.dto.AuthResponse
import br.edu.ufmt.mothercare.auth.dto.CadastroRequest
import br.edu.ufmt.mothercare.auth.dto.LoginRequest
import br.edu.ufmt.mothercare.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// UC01 - Manter Cadastro. Endpoints expostos apenas via API Gateway (/api/auth/**). */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/registrar")
    fun registrar(@Valid @RequestBody request: CadastroRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))
}
