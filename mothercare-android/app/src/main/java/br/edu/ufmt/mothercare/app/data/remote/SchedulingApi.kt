package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SchedulingApi {
    @POST("consultas")
    suspend fun agendar(@Body request: AgendamentoRequest): AgendamentoResponse
}
