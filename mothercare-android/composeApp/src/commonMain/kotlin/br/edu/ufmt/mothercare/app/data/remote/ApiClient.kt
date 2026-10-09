package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.SessionManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Únicas rotas sem token no gateway — um 401 nelas é "senha errada", não sessão expirada. */
private val ROTAS_PUBLICAS = setOf("/api/auth/login", "/api/auth/registrar")

val jsonApi = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Lê o token a cada requisição (nada de cache): login/logout passam a valer na hora. */
private fun pluginToken(sessionManager: SessionManager) = createClientPlugin("TokenBearer") {
    onRequest { request, _ ->
        sessionManager.obterTokenAtual()?.takeIf { it.isNotBlank() }?.let {
            request.header(HttpHeaders.Authorization, "Bearer $it")
        }
    }
}

object ApiClient {
    fun criar(sessionManager: SessionManager): HttpClient = HttpClient {
        // Status fora de 2xx viram ResponseException (tratada em chamarApi).
        expectSuccess = true

        install(ContentNegotiation) { json(jsonApi) }

        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000 // upload de laudo até 10 MB
        }

        install(Logging) {
            level = LogLevel.HEADERS
            sanitizeHeader { it == HttpHeaders.Authorization }
            logger = object : Logger {
                override fun log(message: String) = Napier.d(message, tag = "Ktor")
            }
        }

        install(DefaultRequest) {
            header(HttpHeaders.Accept, "application/json")
            url(baseUrl)
        }

        install(pluginToken(sessionManager))

        install(HttpCallValidator) {
            handleResponseExceptionWithRequest { cause, request ->
                val resposta = (cause as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
                if (resposta.status == HttpStatusCode.Unauthorized &&
                    request.url.encodedPath !in ROTAS_PUBLICAS &&
                    !sessionManager.obterTokenAtual().isNullOrBlank()
                ) {
                    sessionManager.expirarSessao()
                }
            }
        }
    }
}
