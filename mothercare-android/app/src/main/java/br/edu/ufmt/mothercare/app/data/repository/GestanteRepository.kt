package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.CadastroGestanteRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse

class GestanteRepository(private val api: ClinicalApi) {
    suspend fun cadastrar(
        usuarioId: String,
        dataUltimaMenstruacao: String,
        pesoInicialKg: Double,
        alturaMetros: Double
    ): Resultado<IdadeGestacionalResponse> = chamarApi {
        api.cadastrarGestante(CadastroGestanteRequest(usuarioId, dataUltimaMenstruacao, pesoInicialKg, alturaMetros))
    }

    suspend fun obterIdadeGestacional(gestanteId: String): Resultado<IdadeGestacionalResponse> = chamarApi {
        api.obterIdadeGestacional(gestanteId)
    }
}
