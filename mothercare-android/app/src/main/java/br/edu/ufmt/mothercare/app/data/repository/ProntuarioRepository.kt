package br.edu.ufmt.mothercare.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.exigirSucesso
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.DocumentoClinicoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/** Mesmos tipos e limite aceitos pelo clinical-service (ProntuarioDocumentoService / application.yml). */
val TIPOS_DOCUMENTO_ACEITOS = arrayOf("application/pdf", "image/jpeg", "image/png")
private const val TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024

class ProntuarioRepository(
    private val api: ClinicalApi,
    private val contentResolver: ContentResolver,
    private val pastaCache: File
) {

    /** Copia o arquivo escolhido (fora da main thread), envia com o nome original e apaga a cópia. */
    suspend fun enviar(gestanteId: String, categoria: String, uri: Uri): Resultado<DocumentoClinicoResponse> {
        val mimeType = contentResolver.getType(uri)
        if (mimeType !in TIPOS_DOCUMENTO_ACEITOS) {
            return Resultado.Erro(null, "Formato não aceito. Envie um PDF, JPEG ou PNG.")
        }
        val (nomeOriginal, tamanho) = lerMetadados(uri)
        if (tamanho != null && tamanho > TAMANHO_MAXIMO_BYTES) {
            return Resultado.Erro(null, "Arquivo maior que 10 MB.")
        }

        val temporario = File(pastaCache, "upload_${System.currentTimeMillis()}")
        return try {
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { entrada ->
                    temporario.outputStream().use { saida -> entrada.copyTo(saida) }
                } ?: error("Não foi possível ler o arquivo selecionado")
            }
            chamarApi {
                val gestanteIdBody = gestanteId.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoriaBody = categoria.toRequestBody("text/plain".toMediaTypeOrNull())
                val arquivoBody = temporario.asRequestBody(mimeType!!.toMediaTypeOrNull())
                val parteArquivo = MultipartBody.Part.createFormData("arquivo", nomeOriginal, arquivoBody)
                api.enviarDocumento(gestanteIdBody, categoriaBody, parteArquivo)
            }
        } catch (e: Exception) {
            Resultado.Erro(null, e.message ?: "Falha ao ler o arquivo selecionado")
        } finally {
            temporario.delete()
        }
    }

    suspend fun listar(gestanteId: String): Resultado<List<DocumentoClinicoResponse>> = chamarApi {
        api.listarDocumentos(gestanteId)
    }

    suspend fun excluir(documentoId: String): Resultado<Unit> = chamarApi {
        api.excluirDocumento(documentoId).exigirSucesso()
    }

    /** Baixa o laudo para o cache (pasta exposta pelo FileProvider) para abrir num visualizador externo. */
    suspend fun baixar(documento: DocumentoClinicoResponse): Resultado<File> = chamarApi {
        val pasta = File(pastaCache, "documentos").apply { mkdirs() }
        val destino = File(pasta, "${documento.id}_${documento.nomeArquivoOriginal.replace(Regex("[^\\w.\\-]"), "_")}")
        withContext(Dispatchers.IO) {
            val corpo = api.baixarDocumento(documento.id)
            corpo.use { destino.outputStream().use { saida -> it.byteStream().copyTo(saida) } }
        }
        destino
    }

    private fun lerMetadados(uri: Uri): Pair<String, Long?> {
        var nome = "documento"
        var tamanho: Long? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val iNome = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val iTamanho = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (iNome >= 0 && !cursor.isNull(iNome)) nome = cursor.getString(iNome)
                if (iTamanho >= 0 && !cursor.isNull(iTamanho)) tamanho = cursor.getLong(iTamanho)
            }
        }
        return nome to tamanho
    }
}
