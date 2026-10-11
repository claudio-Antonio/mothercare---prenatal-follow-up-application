package br.edu.ufmt.mothercare.app.data.remote

import br.edu.ufmt.mothercare.app.data.ArquivoEscolhido
import br.edu.ufmt.mothercare.app.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class ClinicalApi(private val client: HttpClient) {
    suspend fun cadastrarGestante(request: CadastroGestanteRequest): IdadeGestacionalResponse {
        return client.post("gestantes") {
            setBody(request)
        }.body()
    }

    suspend fun minhaGestante(): IdadeGestacionalResponse {
        return client.get("gestantes/me").body()
    }

    suspend fun atualizarMinhaGestante(request: AtualizarGestanteRequest): IdadeGestacionalResponse {
        return client.put("gestantes/me") {
            setBody(request)
        }.body()
    }

    suspend fun desvincularMedico() {
        client.delete("gestantes/me/medico")
    }

    suspend fun obterIdadeGestacional(gestanteId: String): IdadeGestacionalResponse {
        return client.get("gestantes/$gestanteId/idade-gestacional").body()
    }

    suspend fun obterChecklist(gestanteId: String): ChecklistExamesResponse {
        return client.get("exames/checklist/$gestanteId").body()
    }

    suspend fun checkInPressao(request: CheckInPressaoRequest): ResultadoTriagemPressaoResponse {
        return client.post("checkin/pressao") {
            setBody(request)
        }.body()
    }

    /** RF08: notificações do usuário logado (gestante ou médico), da mais recente para a mais antiga. */
    suspend fun notificacoes(): List<NotificacaoResponse> {
        return client.get("notificacoes").body()
    }

    suspend fun marcarNotificacaoLida(id: String): NotificacaoResponse {
        return client.patch("notificacoes/$id/lida").body()
    }

    suspend fun ultimoStatusPressao(gestanteId: String): UltimoStatusPressaoResponse {
        return client.get("checkin/pressao/$gestanteId/ultimo-status").body()
    }

    suspend fun historicoPressao(gestanteId: String): List<RegistroPressaoResponse> {
        return client.get("checkin/pressao/$gestanteId").body()
    }

    suspend fun historicoExames(gestanteId: String): List<ExameHistoricoResponse> {
        return client.get("exames/$gestanteId").body()
    }

    suspend fun registrarExame(request: ExameLaboratorialRequest): ExameLaboratorialResponse {
        return client.post("exames") {
            setBody(request)
        }.body()
    }

    suspend fun registrarPeso(request: RegistroPesoRequest): GraficoPesoResponse {
        return client.post("peso") {
            setBody(request)
        }.body()
    }

    suspend fun obterGraficoPeso(gestanteId: String): GraficoPesoResponse {
        return client.get("peso/$gestanteId/grafico").body()
    }

    /** multipart/form-data: gestanteId + categoria + arquivo (nomes esperados pelo ProntuarioController). */
    suspend fun enviarDocumento(
        gestanteId: String,
        categoria: String,
        arquivo: ArquivoEscolhido
    ): DocumentoClinicoResponse {
        return client.submitFormWithBinaryData(
            url = "prontuario/documentos",
            formData = formData {
                append("gestanteId", gestanteId)
                append("categoria", categoria)
                append("arquivo", arquivo.bytes, Headers.build {
                    append(HttpHeaders.ContentType, arquivo.mimeType ?: "application/octet-stream")
                    append(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.File.withParameter(ContentDisposition.Parameters.FileName, arquivo.nome).toString()
                    )
                })
            }
        ).body()
    }

    suspend fun listarDocumentos(gestanteId: String): List<DocumentoClinicoResponse> {
        return client.get("prontuario/documentos/$gestanteId").body()
    }

    suspend fun excluirDocumento(documentoId: String) {
        client.delete("prontuario/documentos/$documentoId")
    }

    suspend fun baixarDocumento(documentoId: String): ByteArray {
        return client.get("prontuario/documentos/conteudo/$documentoId").body()
    }

    suspend fun listarMinhasGestantes(): List<GestanteResumoResponse> {
        return client.get("gestantes/medico/minhas-gestantes").body()
    }

    suspend fun vincularGestante(request: VincularGestanteRequest) {
        client.post("gestantes/medico/vincular") {
            setBody(request)
        }
    }
}
