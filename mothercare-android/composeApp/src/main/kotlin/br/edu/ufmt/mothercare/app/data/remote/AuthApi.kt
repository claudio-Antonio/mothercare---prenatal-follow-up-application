package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.AuthResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.CadastroRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.LoginRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(private val client: HttpClient) {
    suspend fun registrar(request: CadastroRequest): AuthResponse {
        return client.post("auth/registrar") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        return client.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
