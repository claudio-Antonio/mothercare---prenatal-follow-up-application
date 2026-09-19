package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.AuthResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.CadastroRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.LoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/registrar")
    suspend fun registrar(@Body request: CadastroRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}
