package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ClinicalApi {
    @POST("gestantes")
    suspend fun cadastrarGestante(@Body request: CadastroGestanteRequest): IdadeGestacionalResponse

    /** Cadastro clínico do usuário do token: 200 → já cadastrada; 404 → ainda não. */
    @GET("gestantes/me")
    suspend fun minhaGestante(): IdadeGestacionalResponse

    @PUT("gestantes/me")
    suspend fun atualizarMinhaGestante(@Body request: AtualizarGestanteRequest): IdadeGestacionalResponse

    @DELETE("gestantes/me/medico")
    suspend fun desvincularMedico(): Response<Unit>

    @GET("gestantes/{id}/idade-gestacional")
    suspend fun obterIdadeGestacional(@Path("id") gestanteId: String): IdadeGestacionalResponse

    @GET("exames/checklist/{gestanteId}")
    suspend fun obterChecklist(@Path("gestanteId") gestanteId: String): ChecklistExamesResponse

    @POST("checkin/pressao")
    suspend fun checkInPressao(@Body request: CheckInPressaoRequest): ResultadoTriagemPressaoResponse

    @GET("checkin/pressao/{gestanteId}/ultimo-status")
    suspend fun ultimoStatusPressao(@Path("gestanteId") gestanteId: String): UltimoStatusPressaoResponse

    @GET("checkin/pressao/{gestanteId}")
    suspend fun historicoPressao(@Path("gestanteId") gestanteId: String): List<RegistroPressaoResponse>

    @GET("exames/{gestanteId}")
    suspend fun historicoExames(@Path("gestanteId") gestanteId: String): List<ExameHistoricoResponse>

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

    @DELETE("prontuario/documentos/{id}")
    suspend fun excluirDocumento(@Path("id") documentoId: String): Response<Unit>

    @Streaming
    @GET("prontuario/documentos/conteudo/{id}")
    suspend fun baixarDocumento(@Path("id") documentoId: String): okhttp3.ResponseBody

    // Painel do médico (UC07)
    @GET("gestantes/medico/minhas-gestantes")
    suspend fun listarMinhasGestantes(): List<GestanteResumoResponse>

    @POST("gestantes/medico/vincular")
    suspend fun vincularGestante(@Body request: VincularGestanteRequest): Response<Unit>
}
