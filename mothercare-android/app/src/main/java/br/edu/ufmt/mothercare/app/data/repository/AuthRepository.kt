package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.SessionManager
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.AuthApi
import br.edu.ufmt.mothercare.app.data.remote.dto.AuthResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.CadastroRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.LoginRequest

class AuthRepository(
    private val api: AuthApi,
    private val sessionManager: SessionManager
) {
    suspend fun registrar(nome: String, email: String, senha: String): Resultado<AuthResponse> {
        val resultado = chamarApi { api.registrar(CadastroRequest(nome, email, senha)) }
        if (resultado is Resultado.Sucesso) {
            sessionManager.salvarSessao(resultado.dados.token, resultado.dados.usuario.id, resultado.dados.usuario.nome)
        }
        return resultado
    }

    suspend fun login(email: String, senha: String): Resultado<AuthResponse> {
        val resultado = chamarApi { api.login(LoginRequest(email, senha)) }
        if (resultado is Resultado.Sucesso) {
            sessionManager.salvarSessao(resultado.dados.token, resultado.dados.usuario.id, resultado.dados.usuario.nome)
        }
        return resultado
    }

    suspend fun logout() {
        sessionManager.limparSessao()
    }
}
