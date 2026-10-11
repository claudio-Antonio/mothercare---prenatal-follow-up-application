package br.edu.ufmt.mothercare.app.data

import br.edu.ufmt.mothercare.app.data.remote.dto.ErroResponse
import br.edu.ufmt.mothercare.app.data.remote.jsonApi
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException

sealed class Resultado<out T> {
    data class Sucesso<T>(val dados: T) : Resultado<T>()

    /** [codigo]: código de negócio do backend, ex.: RISCO_PA / INTERVALO_MINIMO no agendamento. */
    data class Erro(val codigoHttp: Int?, val mensagem: String, val codigo: String? = null) : Resultado<Nothing>()
}

suspend fun <T> chamarApi(bloco: suspend () -> T): Resultado<T> {
    return try {
        Resultado.Sucesso(bloco())
    } catch (e: ResponseException) {
        val resposta = e.response
        val codigoHttp = resposta.status.value
        val corpoErro = try {
            resposta.body<String>()
        } catch (ex: Exception) {
            ""
        }
        val erro = try {
            corpoErro.takeIf { it.isNotBlank() }?.let {
                jsonApi.decodeFromString<ErroResponse>(it)
            }
        } catch (parseEx: Exception) {
            null
        }
        // 401/403 do gateway e do Spring Security vêm com corpo vazio; o gateway põe o motivo em X-Auth-Error.
        val mensagem = erro?.mensagem
            ?: resposta.headers["X-Auth-Error"]
            ?: mensagemPadrao(codigoHttp, erro?.error)
        Resultado.Erro(codigoHttp, mensagem, erro?.codigo)
    } catch (e: Exception) {
        Resultado.Erro(null, "Falha de conexão. Verifique se o backend está no ar.")
    }
}

private fun mensagemPadrao(codigo: Int, erroSpring: String?): String = when (codigo) {
    401 -> "Sua sessão expirou. Faça login novamente."
    403 -> "Você não tem permissão para acessar este recurso."
    404 -> "Recurso não encontrado."
    413 -> "Arquivo maior que o limite permitido (10 MB)."
    in 500..599 -> "O servidor está indisponível no momento. Tente novamente em instantes."
    else -> "Erro $codigo${erroSpring?.let { ": $it" } ?: ""}"
}
