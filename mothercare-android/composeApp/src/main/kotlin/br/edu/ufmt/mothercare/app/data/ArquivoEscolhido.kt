package br.edu.ufmt.mothercare.app.data

/** Arquivo devolvido pelo seletor de arquivos da plataforma: já lido em memória (limite de 10 MB no backend). */
class ArquivoEscolhido(
    val nome: String,
    val mimeType: String?,
    val bytes: ByteArray
)
