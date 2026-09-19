package br.edu.ufmt.mothercare.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val usuarioId: String? = null,
    val loginConcluido: Boolean = false
)

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) { _uiState.value = _uiState.value.copy(email = valor) }
    fun onSenhaChange(valor: String) { _uiState.value = _uiState.value.copy(senha = valor) }

    fun login() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val resultado = repository.login(_uiState.value.email.trim(), _uiState.value.senha)
            _uiState.value = when (resultado) {
                is Resultado.Sucesso -> _uiState.value.copy(carregando = false, usuarioId = resultado.dados.usuario.id, loginConcluido = true)
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
