package br.edu.ufmt.mothercare.app.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.ResultadoTriagemPressaoResponse
import br.edu.ufmt.mothercare.app.data.repository.CheckInRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckInUiState(
    val sistolica: String = "",
    val diastolica: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val resultado: ResultadoTriagemPressaoResponse? = null
)

class CheckInViewModel(
    private val gestanteId: String,
    private val repository: CheckInRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    fun onSistolicaChange(valor: String) { _uiState.value = _uiState.value.copy(sistolica = valor, resultado = null) }
    fun onDiastolicaChange(valor: String) { _uiState.value = _uiState.value.copy(diastolica = valor, resultado = null) }

    fun registrar() {
        val sistolica = _uiState.value.sistolica.toIntOrNull()
        val diastolica = _uiState.value.diastolica.toIntOrNull()
        if (sistolica == null || diastolica == null) {
            _uiState.value = _uiState.value.copy(erro = "Preencha sistólica e diastólica com números válidos.")
            return
        }
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            _uiState.value = when (val resultado = repository.registrarPressao(gestanteId, sistolica, diastolica)) {
                is Resultado.Sucesso -> _uiState.value.copy(carregando = false, resultado = resultado.dados)
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
