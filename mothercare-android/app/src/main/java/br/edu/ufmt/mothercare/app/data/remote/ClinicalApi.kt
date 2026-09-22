package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ClinicalApi {
    @POST("gestantes")
    suspend fun cadastrarGestante(@Body request: CadastroGestanteRequest): IdadeGestacionalResponse

    @GET("gestantes/{id}/idade-gestacional")
    suspend fun obterIdadeGestacional(@Path("id") gestanteId: String): IdadeGestacionalResponse

    @GET("exames/checklist/{gestanteId}")
    suspend fun obterChecklist(@Path("gestanteId") gestanteId: String): ChecklistExamesResponse

    @POST("checkin/pressao")
    suspend fun checkInPressao(@Body request: CheckInPressaoRequest): ResultadoTriagemPressaoResponse

    @POST("exames")
    suspend fun registrarExame(@Body request: ExameLaboratorialRequest): ExameLaboratorialResponse

    @POST("peso")
    suspend fun registrarPeso(@Body request: RegistroPesoRequest): GraficoPesoResponse

    @GET("peso/{gestanteId}/grafico")
    suspend fun obterGraficoPeso(@Path("gestanteId") gestanteId: String): GraficoPesoResponse

    @Multipart
    @POST("prontuario/documentos")
    suspend fun enviarDocumento(
        @Part("gestanteId") gestanteId: RequestBody,
        @Part("categoria") categoria: RequestBody,
        @Part arquivo: MultipartBody.Part
    ): DocumentoClinicoResponse

    @GET("prontuario/documentos/{gestanteId}")
    suspend fun listarDocumentos(@Path("gestanteId") gestanteId: String): List<DocumentoClinicoResponse>

    @Streaming
    @GET("prontuario/documentos/conteudo/{id}")
    suspend fun baixarDocumento(@Path("id") documentoId: String): okhttp3.ResponseBody
}
