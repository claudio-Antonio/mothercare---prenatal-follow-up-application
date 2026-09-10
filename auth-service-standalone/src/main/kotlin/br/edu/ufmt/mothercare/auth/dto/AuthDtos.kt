package br.edu.ufmt.mothercare.auth.dto

import br.edu.ufmt.mothercare.auth.entity.Papel
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** RF01: cadastro inicial da gestante (nome, email e senha). */
data class CadastroRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    val nome: String,

    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
    val senha: String,

    val papel: Papel = Papel.GESTANTE
)

data class LoginRequest(
    @field:NotBlank @field:Email
    val email: String,

    @field:NotBlank
    val senha: String
)

data class AuthResponse(
    val token: String,
    val tipo: String = "Bearer",
    val usuario: UsuarioResponse
)

data class UsuarioResponse(
    val id: String,
    val nome: String,
    val email: String,
    val papel: Papel
)
