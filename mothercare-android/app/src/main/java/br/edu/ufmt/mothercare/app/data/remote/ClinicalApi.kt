package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ClinicalApi {
    @POST("gestantes")
    suspend fun cadastrarGestante(@Body request: CadastroGestanteRequest): IdadeGestacionalResponse

    @GET("gestantes/{id}/idade-gestacional")
    suspend fun obterIdadeGestacional(@Path("id") gestanteId: String): IdadeGestacionalResponse

    @GET("exames/checklist/{gestanteId}")
    suspend fun obterChecklist(@Path("gestanteId") gestanteId: String): ChecklistExamesResponse

    @POST("checkin/pressao")
    suspend fun checkInPressao(@Body request: CheckInPressaoRequest): ResultadoTriagemPressaoResponse
}
