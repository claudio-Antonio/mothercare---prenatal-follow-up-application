package br.edu.ufmt.mothercare.app.data.remote.dto

enum class Papel { GESTANTE, MEDICO }

data class CadastroRequest(
    val nome: String,
    val email: String,
    val senha: String,
    val papel: Papel = Papel.GESTANTE
)

data class LoginRequest(
    val email: String,
    val senha: String
)

data class UsuarioResponse(
    val id: String,
    val nome: String,
    val email: String,
    val papel: Papel
)

data class AuthResponse(
    val token: String,
    val tipo: String,
    val usuario: UsuarioResponse
)
