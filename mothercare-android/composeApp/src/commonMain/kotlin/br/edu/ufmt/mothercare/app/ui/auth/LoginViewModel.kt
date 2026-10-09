package br.edu.ufmt.mothercare.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.Papel
import br.edu.ufmt.mothercare.app.data.repository.AuthRepository
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Para onde ir depois do login — evento consumido pela navegação. */
sealed class DestinoLogin {
    data object PainelMedico : DestinoLogin()
    data class HomeGestante(val gestanteId: String) : DestinoLogin()
    data class CadastroGestante(val usuarioId: String) : DestinoLogin()
}

data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val destino: DestinoLogin? = null
)

class LoginViewModel(
    private val repository: AuthRepository,
    private val gestanteRepository: GestanteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) { _uiState.value = _uiState.value.copy(email = valor) }
    fun onSenhaChange(valor: String) { _uiState.value = _uiState.value.copy(senha = valor) }

    /** Zera o evento depois de navegar; sem isso, voltar pra esta tela navegaria de novo. */
    fun navegacaoConsumida() { _uiState.value = _uiState.value.copy(destino = null) }

    fun login() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val resultado = repository.login(_uiState.value.email.trim(), _uiState.value.senha)
            _uiState.value = when (resultado) {
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = resultado.mensagem)
                is Resultado.Sucesso -> {
                    val usuario = resultado.dados.usuario
                    when (usuario.papel) {
                        Papel.MEDICO -> _uiState.value.copy(carregando = false, destino = DestinoLogin.PainelMedico)
                        Papel.GESTANTE -> resolverDestinoGestante(usuario.id)
                    }
                }
            }
        }
    }

    /** Gestante que já fez o cadastro clínico vai direto pra home (antes caía de novo no cadastro → 409). */
    private suspend fun resolverDestinoGestante(usuarioId: String): LoginUiState =
        when (val gestante = gestanteRepository.buscarMinha()) {
            is Resultado.Sucesso -> _uiState.value.copy(carregando = false, destino = DestinoLogin.HomeGestante(gestante.dados.gestanteId))
            is Resultado.Erro ->
                if (gestante.codigoHttp == 404) _uiState.value.copy(carregando = false, destino = DestinoLogin.CadastroGestante(usuarioId))
                else _uiState.value.copy(carregando = false, erro = gestante.mensagem)
        }
}
