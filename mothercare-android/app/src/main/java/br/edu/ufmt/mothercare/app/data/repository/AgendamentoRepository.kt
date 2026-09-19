package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.SchedulingApi
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse

class AgendamentoRepository(private val api: SchedulingApi) {
    suspend fun agendar(gestanteId: String, dataHoraDesejada: String): Resultado<AgendamentoResponse> = chamarApi {
        api.agendar(AgendamentoRequest(gestanteId, dataHoraDesejada))
    }
}
