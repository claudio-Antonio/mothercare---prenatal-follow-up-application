package br.edu.ufmt.mothercare.app.ui.prontuario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.ArquivoEscolhido
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.app.data.repository.ProntuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProntuarioUiState(
    val carregandoLista: Boolean = true,
    val enviando: Boolean = false,
    val baixandoId: String? = null,
    val erro: String? = null,
    val documentos: List<DocumentoClinicoResponse> = emptyList(),
    val mensagemSucesso: String? = null,
    val arquivoParaAbrir: ArquivoParaAbrir? = null
)

/** UC07 - Gerenciar Prontuário Digital. */
class ProntuarioViewModel(
    private val gestanteId: String,
    private val repository: ProntuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProntuarioUiState())
    val uiState: StateFlow<ProntuarioUiState> = _uiState.asStateFlow()

    init { carregarLista() }

    fun carregarLista() {
        _uiState.value = _uiState.value.copy(carregandoLista = true, erro = null)
        viewModelScope.launch {
            when (val resultado = repository.listar(gestanteId)) {
                is Resultado.Sucesso -> _uiState.value = _uiState.value.copy(carregandoLista = false, documentos = resultado.dados)
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(carregandoLista = false, erro = resultado.mensagem)
            }
        }
    }

    fun enviarDocumento(categoria: String, arquivo: ArquivoEscolhido) {
        if (categoria.isBlank()) {
            _uiState.value = _uiState.value.copy(erro = "Informe uma categoria pro documento (ex: Hemograma completo).")
            return
        }
        _uiState.value = _uiState.value.copy(enviando = true, erro = null, mensagemSucesso = null)
        viewModelScope.launch {
            when (val resultado = repository.enviar(gestanteId, categoria.trim(), arquivo)) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(enviando = false, mensagemSucesso = "Documento enviado com sucesso.")
                    carregarLista()
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(enviando = false, erro = resultado.mensagem)
            }
        }
    }

    fun abrirDocumento(documento: DocumentoClinicoResponse) {
        if (_uiState.value.baixandoId != null) return
        _uiState.value = _uiState.value.copy(baixandoId = documento.id, erro = null)
        viewModelScope.launch {
            when (val resultado = repository.baixar(documento)) {
                is Resultado.Sucesso -> _uiState.value = _uiState.value.copy(
                    baixandoId = null,
                    arquivoParaAbrir = ArquivoParaAbrir(documento.nomeArquivoOriginal, mimeTypeDoDocumento(documento.tipoConteudo), resultado.dados)
                )
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(baixandoId = null, erro = resultado.mensagem)
            }
        }
    }

    fun excluir(documento: DocumentoClinicoResponse) {
        _uiState.value = _uiState.value.copy(erro = null, mensagemSucesso = null)
        viewModelScope.launch {
            when (val resultado = repository.excluir(documento.id)) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(mensagemSucesso = "Documento excluído.")
                    carregarLista()
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(erro = resultado.mensagem)
            }
        }
    }

    fun arquivoAberto() { _uiState.value = _uiState.value.copy(arquivoParaAbrir = null) }

    fun informarErro(mensagem: String) { _uiState.value = _uiState.value.copy(erro = mensagem) }
}
