package br.edu.ufmt.mothercare.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val dados: IdadeGestacionalResponse? = null
)

class DashboardViewModel(
    private val gestanteId: String,
    private val repository: GestanteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { carregar() }

    fun carregar() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            _uiState.value = when (val resultado = repository.obterIdadeGestacional(gestanteId)) {
                is Resultado.Sucesso -> DashboardUiState(carregando = false, dados = resultado.dados)
                is Resultado.Erro -> DashboardUiState(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
