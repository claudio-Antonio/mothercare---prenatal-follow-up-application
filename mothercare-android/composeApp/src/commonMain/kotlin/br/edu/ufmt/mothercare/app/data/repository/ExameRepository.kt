package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameHistoricoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameLaboratorialRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameLaboratorialResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.TipoExame

class ExameRepository(private val api: ClinicalApi) {
    /** [documentoId]: laudo já enviado ao prontuário de onde o valor foi tirado (opcional). */
    suspend fun registrar(gestanteId: String, tipo: TipoExame, valor: Double, documentoId: String? = null): Resultado<ExameLaboratorialResponse> =
        chamarApi { api.registrarExame(ExameLaboratorialRequest(gestanteId, tipo, valor, documentoId)) }

    suspend fun historico(gestanteId: String): Resultado<List<ExameHistoricoResponse>> =
        chamarApi { api.historicoExames(gestanteId) }
}
