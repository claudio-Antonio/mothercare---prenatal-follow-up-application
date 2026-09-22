package br.edu.ufmt.mothercare.app.ui.exame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameLaboratorialResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.TipoExame
import br.edu.ufmt.mothercare.app.data.repository.ExameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExameUiState(
    val tipo: TipoExame = TipoExame.HEMOGLOBINA,
    val valorInput: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val resultado: ExameLaboratorialResponse? = null
)

/** UC08 - Emitir Alertas (RN03: Hb < 11 g/dL, Glicemia de jejum >= 92 mg/dL). */
class ExameViewModel(
    private val gestanteId: String,
    private val repository: ExameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExameUiState())
    val uiState: StateFlow<ExameUiState> = _uiState.asStateFlow()

    fun onTipoChange(tipo: TipoExame) { _uiState.value = _uiState.value.copy(tipo = tipo, resultado = null) }
    fun onValorChange(valor: String) { _uiState.value = _uiState.value.copy(valorInput = valor, resultado = null) }

    fun registrar() {
        val valor = _uiState.value.valorInput.replace(",", ".").toDoubleOrNull()
        if (valor == null) {
            _uiState.value = _uiState.value.copy(erro = "Informe um valor numérico válido.")
            return
        }
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            when (val resultado = repository.registrar(gestanteId, _uiState.value.tipo, valor)) {
                is Resultado.Sucesso -> _uiState.value = _uiState.value.copy(carregando = false, resultado = resultado.dados, valorInput = "")
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
