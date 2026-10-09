package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.SessionManager
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.exigirSucesso
import br.edu.ufmt.mothercare.app.data.remote.dto.AtualizarGestanteRequest
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse

class GestanteRepository(
    private val api: ClinicalApi,
    private val sessionManager: SessionManager
) {
    suspend fun cadastrar(
        usuarioId: String,
        dataUltimaMenstruacao: String,
        pesoInicialKg: Double,
        alturaMetros: Double
    ): Resultado<IdadeGestacionalResponse> {
        val resultado = chamarApi {
            api.cadastrarGestante(CadastroGestanteRequest(usuarioId, dataUltimaMenstruacao, pesoInicialKg, alturaMetros))
        }
        if (resultado is Resultado.Sucesso) sessionManager.salvarGestanteId(resultado.dados.gestanteId)
        return resultado
    }

    /** Usado após o login: 404 significa que a gestante ainda precisa do cadastro clínico. */
    suspend fun buscarMinha(): Resultado<IdadeGestacionalResponse> {
        val resultado = chamarApi { api.minhaGestante() }
        if (resultado is Resultado.Sucesso) sessionManager.salvarGestanteId(resultado.dados.gestanteId)
        return resultado
    }

    suspend fun obterIdadeGestacional(gestanteId: String): Resultado<IdadeGestacionalResponse> = chamarApi {
        api.obterIdadeGestacional(gestanteId)
    }

    suspend fun atualizar(dataUltimaMenstruacao: String, pesoInicialKg: Double, alturaMetros: Double): Resultado<IdadeGestacionalResponse> =
        chamarApi { api.atualizarMinhaGestante(AtualizarGestanteRequest(dataUltimaMenstruacao, pesoInicialKg, alturaMetros)) }

    suspend fun desvincularMedico(): Resultado<Unit> = chamarApi { api.desvincularMedico().exigirSucesso() }
}
