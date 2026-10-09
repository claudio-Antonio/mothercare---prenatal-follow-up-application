package br.edu.ufmt.mothercare.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.UltimoStatusPressaoResponse
import br.edu.ufmt.mothercare.app.data.repository.CheckInRepository
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val dados: IdadeGestacionalResponse? = null,
    /** Último check-in de PA (RF05). null = não carregou; o card simplesmente não aparece. */
    val statusPressao: UltimoStatusPressaoResponse? = null
)

class DashboardViewModel(
    private val gestanteId: String,
    private val repository: GestanteRepository,
    private val checkInRepository: CheckInRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun carregar() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val pressao = async { checkInRepository.ultimoStatus(gestanteId) }
            val idade = repository.obterIdadeGestacional(gestanteId)
            val statusPressao = (pressao.await() as? Resultado.Sucesso)?.dados
            _uiState.value = when (idade) {
                is Resultado.Sucesso -> DashboardUiState(carregando = false, dados = idade.dados, statusPressao = statusPressao)
                is Resultado.Erro -> DashboardUiState(carregando = false, erro = idade.mensagem)
            }
        }
    }
}
