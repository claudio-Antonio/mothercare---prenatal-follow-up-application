package br.edu.ufmt.mothercare.app.ui.prontuario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.app.data.repository.ProntuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ProntuarioUiState(
    val carregandoLista: Boolean = true,
    val enviando: Boolean = false,
    val erro: String? = null,
    val documentos: List<DocumentoClinicoResponse> = emptyList(),
    val mensagemSucesso: String? = null
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

    fun enviarDocumento(categoria: String, arquivoLocal: File, mimeType: String) {
        if (categoria.isBlank()) {
            _uiState.value = _uiState.value.copy(erro = "Informe uma categoria pro documento (ex: Hemograma completo).")
            return
        }
        _uiState.value = _uiState.value.copy(enviando = true, erro = null, mensagemSucesso = null)
        viewModelScope.launch {
            when (val resultado = repository.enviar(gestanteId, categoria, arquivoLocal, mimeType)) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(enviando = false, mensagemSucesso = "Documento enviado com sucesso.")
                    carregarLista()
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(enviando = false, erro = resultado.mensagem)
            }
        }
    }
}
