package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.CheckInPressaoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.ResultadoTriagemPressaoResponse

class CheckInRepository(private val api: ClinicalApi) {
    suspend fun registrarPressao(gestanteId: String, sistolica: Int, diastolica: Int): Resultado<ResultadoTriagemPressaoResponse> =
        chamarApi { api.checkInPressao(CheckInPressaoRequest(gestanteId, sistolica, diastolica)) }
}
