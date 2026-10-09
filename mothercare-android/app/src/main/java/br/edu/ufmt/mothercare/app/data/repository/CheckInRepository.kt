package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.CheckInPressaoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.RegistroPressaoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ResultadoTriagemPressaoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.UltimoStatusPressaoResponse

class CheckInRepository(private val api: ClinicalApi) {
    suspend fun registrarPressao(gestanteId: String, sistolica: Int, diastolica: Int): Resultado<ResultadoTriagemPressaoResponse> =
        chamarApi { api.checkInPressao(CheckInPressaoRequest(gestanteId, sistolica, diastolica)) }

    suspend fun ultimoStatus(gestanteId: String): Resultado<UltimoStatusPressaoResponse> =
        chamarApi { api.ultimoStatusPressao(gestanteId) }

    suspend fun historico(gestanteId: String): Resultado<List<RegistroPressaoResponse>> =
        chamarApi { api.historicoPressao(gestanteId) }
}
