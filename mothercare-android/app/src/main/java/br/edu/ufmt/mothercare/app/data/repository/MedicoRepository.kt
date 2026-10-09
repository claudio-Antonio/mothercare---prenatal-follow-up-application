package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.exigirSucesso
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.GestanteResumoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.VincularGestanteRequest

class MedicoRepository(private val api: ClinicalApi) {
    suspend fun listarMinhasGestantes(): Resultado<List<GestanteResumoResponse>> = chamarApi {
        api.listarMinhasGestantes()
    }

    suspend fun vincularGestante(codigoAcompanhamento: String): Resultado<Unit> = chamarApi {
        api.vincularGestante(VincularGestanteRequest(codigoAcompanhamento.trim())).exigirSucesso()
    }
}
