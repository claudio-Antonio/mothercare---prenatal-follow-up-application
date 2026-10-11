package br.edu.ufmt.mothercare.app.ui.components

import br.edu.ufmt.mothercare.app.data.Resultado

/** Estado de uma seção carregada da API: telas com várias fontes (ex.: detalhe do médico) têm uma por seção. */
data class Carregavel<out T>(
    val carregando: Boolean = true,
    val dados: T? = null,
    val erro: String? = null
)

fun <T> Resultado<T>.paraCarregavel(): Carregavel<T> = when (this) {
    is Resultado.Sucesso -> Carregavel(carregando = false, dados = dados)
    is Resultado.Erro -> Carregavel(carregando = false, erro = mensagem)
}
