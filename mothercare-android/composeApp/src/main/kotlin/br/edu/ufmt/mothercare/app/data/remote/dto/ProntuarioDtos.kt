package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

@Serializable
data class DocumentoClinicoResponse(
    val id: String,
    val gestanteId: String,
    val categoria: String,
    val nomeArquivoOriginal: String,
    val tipoConteudo: String, // "PDF" | "IMAGEM"
    val tamanhoBytes: Long,
    val enviadoEm: String
)
