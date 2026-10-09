package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.SchedulingApi
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse

class AgendamentoRepository(private val api: SchedulingApi) {
    suspend fun agendar(gestanteId: String, dataHoraDesejada: String): Resultado<AgendamentoResponse> = chamarApi {
        api.agendar(AgendamentoRequest(gestanteId, dataHoraDesejada))
    }

    suspend fun listar(gestanteId: String): Resultado<List<ConsultaResponse>> = chamarApi { api.listar(gestanteId) }

    suspend fun cancelar(consultaId: String): Resultado<ConsultaResponse> = chamarApi { api.cancelar(consultaId) }
}
