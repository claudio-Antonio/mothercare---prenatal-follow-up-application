package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.ChecklistExamesResponse

class ChecklistRepository(private val api: ClinicalApi) {
    suspend fun obterChecklist(gestanteId: String): Resultado<ChecklistExamesResponse> = chamarApi {
        api.obterChecklist(gestanteId)
    }
}
