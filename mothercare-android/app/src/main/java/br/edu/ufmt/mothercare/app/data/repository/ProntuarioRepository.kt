package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.DocumentoClinicoResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ProntuarioRepository(private val api: ClinicalApi) {

    suspend fun enviar(
        gestanteId: String,
        categoria: String,
        arquivoLocal: File,
        mimeType: String
    ): Resultado<DocumentoClinicoResponse> = chamarApi {
        val gestanteIdBody = gestanteId.toRequestBody("text/plain".toMediaTypeOrNull())
        val categoriaBody = categoria.toRequestBody("text/plain".toMediaTypeOrNull())
        val arquivoBody = arquivoLocal.asRequestBody(mimeType.toMediaTypeOrNull())
        val parteArquivo = MultipartBody.Part.createFormData("arquivo", arquivoLocal.name, arquivoBody)

        api.enviarDocumento(gestanteIdBody, categoriaBody, parteArquivo)
    }

    suspend fun listar(gestanteId: String): Resultado<List<DocumentoClinicoResponse>> = chamarApi {
        api.listarDocumentos(gestanteId)
    }

    suspend fun baixar(documentoId: String, destino: File): Resultado<File> = chamarApi {
        val corpo = api.baixarDocumento(documentoId)
        destino.outputStream().use { saida -> corpo.byteStream().copyTo(saida) }
        destino
    }
}
