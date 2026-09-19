package br.edu.ufmt.mothercare.app.ui.gestante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CadastroGestanteUiState(
    val dum: String = "",
    val peso: String = "",
    val altura: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val gestanteId: String? = null,
    val concluido: Boolean = false
)

class CadastroGestanteViewModel(
    private val usuarioId: String,
    private val repository: GestanteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CadastroGestanteUiState())
    val uiState: StateFlow<CadastroGestanteUiState> = _uiState.asStateFlow()

    fun onDumChange(valor: String) { _uiState.value = _uiState.value.copy(dum = valor) }
    fun onPesoChange(valor: String) { _uiState.value = _uiState.value.copy(peso = valor) }
    fun onAlturaChange(valor: String) { _uiState.value = _uiState.value.copy(altura = valor) }

    fun cadastrar() {
        val peso = _uiState.value.peso.replace(",", ".").toDoubleOrNull()
        val altura = _uiState.value.altura.replace(",", ".").toDoubleOrNull()
        if (peso == null || altura == null || _uiState.value.dum.isBlank()) {
            _uiState.value = _uiState.value.copy(erro = "Preencha DUM, peso e altura corretamente.")
            return
        }
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val resultado = repository.cadastrar(usuarioId, _uiState.value.dum.trim(), peso, altura)
            _uiState.value = when (resultado) {
                is Resultado.Sucesso -> _uiState.value.copy(carregando = false, gestanteId = resultado.dados.gestanteId, concluido = true)
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
