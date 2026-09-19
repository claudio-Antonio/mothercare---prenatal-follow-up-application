package br.edu.ufmt.mothercare.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CadastroUiState(
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val usuarioId: String? = null,
    val cadastroConcluido: Boolean = false
)

class CadastroViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CadastroUiState())
    val uiState: StateFlow<CadastroUiState> = _uiState.asStateFlow()

    fun onNomeChange(valor: String) { _uiState.value = _uiState.value.copy(nome = valor) }
    fun onEmailChange(valor: String) { _uiState.value = _uiState.value.copy(email = valor) }
    fun onSenhaChange(valor: String) { _uiState.value = _uiState.value.copy(senha = valor) }

    fun cadastrar() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val resultado = repository.registrar(_uiState.value.nome.trim(), _uiState.value.email.trim(), _uiState.value.senha)
            _uiState.value = when (resultado) {
                is Resultado.Sucesso -> _uiState.value.copy(carregando = false, usuarioId = resultado.dados.usuario.id, cadastroConcluido = true)
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
