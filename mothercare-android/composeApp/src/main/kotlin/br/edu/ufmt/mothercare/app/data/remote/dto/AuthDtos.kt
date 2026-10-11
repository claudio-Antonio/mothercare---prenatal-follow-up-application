package br.edu.ufmt.mothercare.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class Papel { GESTANTE, MEDICO }

@Serializable
data class CadastroRequest(
    val nome: String,
    val email: String,
    val senha: String,
    val papel: Papel = Papel.GESTANTE
)

@Serializable
data class LoginRequest(
    val email: String,
    val senha: String
)

@Serializable
data class UsuarioResponse(
    val id: String,
    val nome: String,
    val email: String,
    val papel: Papel
)

@Serializable
data class AuthResponse(
    val token: String,
    val tipo: String,
    val usuario: UsuarioResponse
)
