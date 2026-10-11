package br.edu.ufmt.mothercare.app.data.repository

import br.edu.ufmt.mothercare.app.data.ArquivoEscolhido
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.chamarApi
import br.edu.ufmt.mothercare.app.data.remote.ClinicalApi
import br.edu.ufmt.mothercare.app.data.remote.dto.DocumentoClinicoResponse

/** Mesmos tipos e limite aceitos pelo clinical-service (ProntuarioDocumentoService / application.yml). */
val TIPOS_DOCUMENTO_ACEITOS = listOf("application/pdf", "image/jpeg", "image/png")
private const val TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024

class ProntuarioRepository(
    private val api: ClinicalApi
) {

    /** Valida tipo e tamanho no cliente (o backend repete a checagem) e envia com o nome original. */
    suspend fun enviar(gestanteId: String, categoria: String, arquivo: ArquivoEscolhido): Resultado<DocumentoClinicoResponse> {
        if (arquivo.mimeType !in TIPOS_DOCUMENTO_ACEITOS) {
            return Resultado.Erro(null, "Formato não aceito. Envie um PDF, JPEG ou PNG.")
        }
        if (arquivo.bytes.size > TAMANHO_MAXIMO_BYTES) {
            return Resultado.Erro(null, "Arquivo maior que 10 MB.")
        }
        return chamarApi { api.enviarDocumento(gestanteId, categoria, arquivo) }
    }

    suspend fun listar(gestanteId: String): Resultado<List<DocumentoClinicoResponse>> = chamarApi {
        api.listarDocumentos(gestanteId)
    }

    suspend fun excluir(documentoId: String): Resultado<Unit> = chamarApi {
        api.excluirDocumento(documentoId)
    }

    /** Baixa o laudo para o cache. */
    suspend fun baixar(documento: DocumentoClinicoResponse): Resultado<ByteArray> = chamarApi {
        api.baixarDocumento(documento.id)
    }
}
