package br.edu.ufmt.mothercare.app.ui.prontuario

import br.edu.ufmt.mothercare.app.data.ArquivoEscolhido
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication

/** Extensões aceitas pelo clinical-service (PDF/JPEG/PNG) — filtro do seletor de arquivos. */
val EXTENSOES_DOCUMENTO_ACEITAS = listOf("pdf", "jpg", "jpeg", "png")

/** Laudo baixado, pronto para abrir num visualizador externo (evento consumido pela tela). */
class ArquivoParaAbrir(val nome: String, val mimeType: String, val bytes: ByteArray)

fun mimeTypeDaExtensao(extensao: String): String? = when (extensao.lowercase()) {
    "pdf" -> "application/pdf"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    else -> null
}

fun mimeTypeDoDocumento(tipoConteudo: String): String = if (tipoConteudo == "PDF") "application/pdf" else "image/*"

/** Lê o arquivo escolhido no FileKit para memória (o backend limita a 10 MB). */
suspend fun PlatformFile.paraArquivoEscolhido(): ArquivoEscolhido =
    ArquivoEscolhido(nome = name, mimeType = mimeTypeDaExtensao(extension), bytes = readBytes())

/** Grava o laudo no cache e abre no app padrão da plataforma. Devolve false se não foi possível abrir. */
suspend fun abrirArquivoExterno(abrir: ArquivoParaAbrir): Boolean = try {
    val destino = FileKit.cacheDir / abrir.nome
    destino.write(abrir.bytes)
    FileKit.openFileWithDefaultApplication(destino)
    true
} catch (e: Exception) {
    false
}
