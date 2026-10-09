package br.edu.ufmt.mothercare.app.data

import br.edu.ufmt.mothercare.app.data.remote.dto.ErroResponse
import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response

sealed class Resultado<out T> {
    data class Sucesso<T>(val dados: T) : Resultado<T>()

    /** [codigo]: código de negócio do backend, ex.: RISCO_PA / INTERVALO_MINIMO no agendamento. */
    data class Erro(val codigoHttp: Int?, val mensagem: String, val codigo: String? = null) : Resultado<Nothing>()
}

suspend fun <T> chamarApi(bloco: suspend () -> T): Resultado<T> {
    return try {
        Resultado.Sucesso(bloco())
    } catch (e: HttpException) {
        val resposta = e.response()
        val corpoErro = resposta?.errorBody()?.string()
        val erro = try {
            corpoErro?.takeIf { it.isNotBlank() }?.let { Gson().fromJson(it, ErroResponse::class.java) }
        } catch (parseEx: Exception) {
            null
        }
        // 401/403 do gateway e do Spring Security vêm com corpo vazio; o gateway põe o motivo em X-Auth-Error.
        val mensagem = erro?.mensagem
            ?: resposta?.headers()?.get("X-Auth-Error")
            ?: mensagemPadrao(e.code(), erro?.error)
        Resultado.Erro(e.code(), mensagem, erro?.codigo)
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

/** Para endpoints 204 (Response<Unit>): converte status de erro na mesma HttpException do Retrofit. */
fun Response<*>.exigirSucesso() {
    if (!isSuccessful) throw HttpException(this)
}
