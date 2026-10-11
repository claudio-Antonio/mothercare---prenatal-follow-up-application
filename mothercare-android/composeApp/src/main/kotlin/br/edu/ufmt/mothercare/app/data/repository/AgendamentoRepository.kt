package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.SchedulingApi
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.DisponibilidadeResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.LembreteConsultaResponse

class AgendamentoRepository(private val api: SchedulingApi) {
    /** UC03 + UC04: a PA vai junto (check-in obrigatório). */
    suspend fun agendar(
        gestanteId: String,
        dataHoraDesejada: String,
        sistolica: Int,
        diastolica: Int
    ): Resultado<AgendamentoResponse> = chamarApi {
        api.agendar(AgendamentoRequest(gestanteId, dataHoraDesejada, sistolica, diastolica))
    }

    suspend fun listar(gestanteId: String): Resultado<List<ConsultaResponse>> = chamarApi { api.listar(gestanteId) }

    suspend fun cancelar(consultaId: String): Resultado<ConsultaResponse> = chamarApi { api.cancelar(consultaId) }

    suspend fun disponibilidade(data: String): Resultado<DisponibilidadeResponse> = chamarApi { api.disponibilidade(data) }

    suspend fun lembrete(gestanteId: String): Resultado<LembreteConsultaResponse> = chamarApi { api.lembrete(gestanteId) }
}
