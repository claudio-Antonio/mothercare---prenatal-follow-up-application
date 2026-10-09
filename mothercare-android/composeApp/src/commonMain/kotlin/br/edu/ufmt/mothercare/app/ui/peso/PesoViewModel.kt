package br.edu.ufmt.mothercare.app.ui.peso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.GraficoPesoResponse
import br.edu.ufmt.mothercare.app.data.repository.PesoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PesoUiState(
    val pesoInput: String = "",
    val carregandoGrafico: Boolean = true,
    val registrando: Boolean = false,
    val erro: String? = null,
    val grafico: GraficoPesoResponse? = null
)

/** UC09 - Monitorar Ganho de Peso (RN05). */
class PesoViewModel(
    private val gestanteId: String,
    private val repository: PesoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PesoUiState())
    val uiState: StateFlow<PesoUiState> = _uiState.asStateFlow()

    init { carregarGrafico() }

    fun onPesoInputChange(valor: String) { _uiState.value = _uiState.value.copy(pesoInput = valor) }

    fun carregarGrafico() {
        _uiState.value = _uiState.value.copy(carregandoGrafico = true, erro = null)
        viewModelScope.launch {
            when (val resultado = repository.obterGrafico(gestanteId)) {
                is Resultado.Sucesso -> _uiState.value = _uiState.value.copy(carregandoGrafico = false, grafico = resultado.dados)
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(carregandoGrafico = false, erro = resultado.mensagem)
            }
        }
    }

    fun registrarPeso() {
        val peso = _uiState.value.pesoInput.replace(",", ".").toDoubleOrNull()
        if (peso == null) {
            _uiState.value = _uiState.value.copy(erro = "Informe um peso válido.")
            return
        }
        _uiState.value = _uiState.value.copy(registrando = true, erro = null)
        viewModelScope.launch {
            when (val resultado = repository.registrar(gestanteId, peso)) {
                is Resultado.Sucesso -> _uiState.value = _uiState.value.copy(registrando = false, pesoInput = "", grafico = resultado.dados)
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(registrando = false, erro = resultado.mensagem)
            }
        }
    }
}
