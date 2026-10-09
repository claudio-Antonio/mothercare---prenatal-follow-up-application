package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoRequest
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SchedulingApi {
    @POST("consultas")
    suspend fun agendar(@Body request: AgendamentoRequest): AgendamentoResponse

    @GET("consultas")
    suspend fun listar(@Query("gestanteId") gestanteId: String): List<ConsultaResponse>

    @PATCH("consultas/{id}/cancelar")
    suspend fun cancelar(@Path("id") consultaId: String): ConsultaResponse
}
