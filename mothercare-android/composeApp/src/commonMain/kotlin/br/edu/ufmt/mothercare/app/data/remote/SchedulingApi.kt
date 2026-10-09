package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.DisponibilidadeResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.LembreteConsultaResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class SchedulingApi(private val client: HttpClient) {
    suspend fun agendar(request: AgendamentoRequest): AgendamentoResponse {
        return client.post("consultas") {
            setBody(request)
        }.body()
    }

    suspend fun listar(gestanteId: String): List<ConsultaResponse> {
        return client.get("consultas?gestanteId=$gestanteId").body()
    }

    suspend fun cancelar(consultaId: String): ConsultaResponse {
        return client.patch("consultas/$consultaId/cancelar").body()
    }

    /** RF03: horários livres da clínica na data (yyyy-MM-dd). */
    suspend fun disponibilidade(data: String): DisponibilidadeResponse {
        return client.get("consultas/disponibilidade?data=$data").body()
    }

    /** Seção 8.3: lembrete de próxima consulta da gestante. */
    suspend fun lembrete(gestanteId: String): LembreteConsultaResponse {
        return client.get("consultas/lembrete?gestanteId=$gestanteId").body()
    }
}
