package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.BuildConfig
import br.edu.ufmt.mothercare.app.data.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = "http://10.0.2.2:8080/api/"

/** Únicas rotas sem token no gateway — um 401 nelas é "senha errada", não sessão expirada. */
private val ROTAS_PUBLICAS = setOf("/api/auth/login", "/api/auth/registrar")

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.tokenAtualBloqueante()
        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        val request = requestBuilder.build()
        val response = chain.proceed(request)

        if (response.code == 401 && !token.isNullOrBlank() && request.url.encodedPath !in ROTAS_PUBLICAS) {
            sessionManager.expirarSessaoBloqueante()
        }
        return response
    }
}

object ApiClient {
    fun criar(sessionManager: SessionManager): Retrofit {
        // HEADERS (e não BODY) pra não gravar a senha do login no Logcat; o token é mascarado.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS) // upload de laudo até 10 MB
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
