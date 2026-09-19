package br.edu.ufmt.mothercare.app.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.ChecklistExamesResponse
import br.edu.ufmt.mothercare.app.data.repository.ChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val dados: ChecklistExamesResponse? = null
)

class ChecklistViewModel(
    private val gestanteId: String,
    private val repository: ChecklistRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    init { carregar() }

    fun carregar() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            _uiState.value = when (val resultado = repository.obterChecklist(gestanteId)) {
                is Resultado.Sucesso -> ChecklistUiState(carregando = false, dados = resultado.dados)
                is Resultado.Erro -> ChecklistUiState(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
