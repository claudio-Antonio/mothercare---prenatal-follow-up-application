package br.edu.ufmt.mothercare.app.ui.prontuario

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

/** Abre o laudo baixado num visualizador externo. Devolve false se não houver app para o tipo. */
fun abrirArquivoExterno(context: Context, abrir: ArquivoParaAbrir): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", abrir.arquivo)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, abrir.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

fun mimeTypeDoDocumento(tipoConteudo: String): String = if (tipoConteudo == "PDF") "application/pdf" else "image/*"
