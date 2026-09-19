package br.edu.ufmt.mothercare.app.ui.agendamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgendamentoUiState(
    val dataHora: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val bloqueadoPorRisco: Boolean = false,
    val resultado: AgendamentoResponse? = null
)

class AgendamentoViewModel(
    private val gestanteId: String,
    private val repository: AgendamentoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgendamentoUiState())
    val uiState: StateFlow<AgendamentoUiState> = _uiState.asStateFlow()

    fun onDataHoraChange(valor: String) { _uiState.value = _uiState.value.copy(dataHora = valor, resultado = null) }

    fun agendar() {
        if (_uiState.value.dataHora.isBlank()) {
            _uiState.value = _uiState.value.copy(erro = "Informe a data e hora desejadas.")
            return
        }
        _uiState.value = _uiState.value.copy(carregando = true, erro = null, bloqueadoPorRisco = false)
        viewModelScope.launch {
            when (val resultado = repository.agendar(gestanteId, _uiState.value.dataHora.trim())) {
                is Resultado.Sucesso -> _uiState.value = _uiState.value.copy(carregando = false, resultado = resultado.dados)
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(
                    carregando = false, erro = resultado.mensagem, bloqueadoPorRisco = resultado.codigoHttp == 409
                )
            }
        }
    }
}
