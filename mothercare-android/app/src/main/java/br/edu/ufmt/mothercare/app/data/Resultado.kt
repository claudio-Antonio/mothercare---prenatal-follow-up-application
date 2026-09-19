package br.edu.ufmt.mothercare.app.data

import br.edu.ufmt.mothercare.app.data.remote.dto.ErroResponse
import com.google.gson.Gson
import retrofit2.HttpException

sealed class Resultado<out T> {
    data class Sucesso<T>(val dados: T) : Resultado<T>()
    data class Erro(val codigoHttp: Int?, val mensagem: String) : Resultado<Nothing>()
}

suspend fun <T> chamarApi(bloco: suspend () -> T): Resultado<T> {
    return try {
        Resultado.Sucesso(bloco())
    } catch (e: HttpException) {
        val corpoErro = e.response()?.errorBody()?.string()
        val mensagem = try {
            corpoErro?.let { Gson().fromJson(it, ErroResponse::class.java) }?.mensagem
        } catch (parseEx: Exception) {
            null
        } ?: "Erro ${e.code()}: ${e.message()}"
        Resultado.Erro(e.code(), mensagem)
    } catch (e: Exception) {
        Resultado.Erro(null, e.message ?: "Falha de conexão. Verifique se o backend está no ar.")
    }
}
