package br.edu.ufmt.mothercare.app.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.RegistroPressaoResponse
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
    val resultado: ResultadoTriagemPressaoResponse? = null,
    val historico: List<RegistroPressaoResponse> = emptyList()
)

class CheckInViewModel(
    private val gestanteId: String,
    private val repository: CheckInRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    init { carregarHistorico() }

    fun onSistolicaChange(valor: String) { _uiState.value = _uiState.value.copy(sistolica = valor, resultado = null) }
    fun onDiastolicaChange(valor: String) { _uiState.value = _uiState.value.copy(diastolica = valor, resultado = null) }

    /** Falha no histórico não bloqueia o check-in: a lista só não aparece. */
    fun carregarHistorico() {
        viewModelScope.launch {
            val resultado = repository.historico(gestanteId)
            if (resultado is Resultado.Sucesso) _uiState.value = _uiState.value.copy(historico = resultado.dados)
        }
    }

    fun registrar() {
        val sistolica = _uiState.value.sistolica.toIntOrNull()
        val diastolica = _uiState.value.diastolica.toIntOrNull()
        if (sistolica == null || diastolica == null) {
            _uiState.value = _uiState.value.copy(erro = "Preencha sistólica e diastólica com números válidos.")
            return
        }
        if (diastolica >= sistolica) {
            _uiState.value = _uiState.value.copy(erro = "A diastólica (mínima) deve ser menor que a sistólica (máxima).")
            return
        }
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            _uiState.value = when (val resultado = repository.registrarPressao(gestanteId, sistolica, diastolica)) {
                is Resultado.Sucesso -> _uiState.value.copy(carregando = false, resultado = resultado.dados, sistolica = "", diastolica = "")
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
            if (_uiState.value.resultado != null) carregarHistorico()
        }
    }
}
