package br.edu.ufmt.mothercare.app.ui.medico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.GestanteResumoResponse
import br.edu.ufmt.mothercare.app.data.repository.MedicoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MedicoDashboardUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val gestantes: List<GestanteResumoResponse> = emptyList(),
    val vinculando: Boolean = false,
    val erroVinculo: String? = null,
    /** true fecha o diálogo de vínculo (evento consumido pela tela). */
    val vinculoConcluido: Boolean = false
)

/** UC07 - "O médico visualiza os dados no painel clínico". */
class MedicoDashboardViewModel(private val repository: MedicoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicoDashboardUiState())
    val uiState: StateFlow<MedicoDashboardUiState> = _uiState.asStateFlow()

    fun carregar() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            when (val resultado = repository.listarMinhasGestantes()) {
                is Resultado.Sucesso -> {
                    // Quem está em alerta (PA crítica ou exame alterado) aparece primeiro.
                    val ordenada = resultado.dados.sortedByDescending {
                        it.ultimoNivelRiscoPA == "CRITICO" || it.possuiAlertaLaboratorial
                    }
                    _uiState.value = _uiState.value.copy(carregando = false, gestantes = ordenada)
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }

    /** Vincula a gestante pelo código de acompanhamento que ela vê na tela inicial do app. */
    fun vincular(codigo: String) {
        if (codigo.isBlank()) {
            _uiState.value = _uiState.value.copy(erroVinculo = "Informe o código de acompanhamento.")
            return
        }
        _uiState.value = _uiState.value.copy(vinculando = true, erroVinculo = null)
        viewModelScope.launch {
            when (val resultado = repository.vincularGestante(codigo)) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(vinculando = false, vinculoConcluido = true)
                    carregar()
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(
                    vinculando = false,
                    erroVinculo = if (resultado.codigoHttp == 400) "Código inválido. Confira com a gestante." else resultado.mensagem
                )
            }
        }
    }

    fun vinculoConsumido() { _uiState.value = _uiState.value.copy(vinculoConcluido = false, erroVinculo = null) }
}
