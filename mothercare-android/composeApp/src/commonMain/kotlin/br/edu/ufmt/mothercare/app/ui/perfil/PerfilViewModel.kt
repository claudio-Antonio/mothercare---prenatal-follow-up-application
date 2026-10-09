package br.edu.ufmt.mothercare.app.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PerfilUiState(
    val carregando: Boolean = true,
    val salvando: Boolean = false,
    val erro: String? = null,
    val mensagemSucesso: String? = null,
    val dados: IdadeGestacionalResponse? = null,
    val dum: String = "",
    val peso: String = "",
    val altura: String = ""
)

/** Perfil da gestante: corrige DUM/peso/altura (PUT /gestantes/me) e gerencia o vínculo com o médico. */
class PerfilViewModel(private val repository: GestanteRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    init { carregar() }

    fun onDumChange(valor: String) { _uiState.value = _uiState.value.copy(dum = valor, mensagemSucesso = null) }
    fun onPesoChange(valor: String) { _uiState.value = _uiState.value.copy(peso = valor, mensagemSucesso = null) }
    fun onAlturaChange(valor: String) { _uiState.value = _uiState.value.copy(altura = valor, mensagemSucesso = null) }

    fun carregar() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            _uiState.value = when (val resultado = repository.buscarMinha()) {
                is Resultado.Sucesso -> preencher(resultado.dados).copy(carregando = false)
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }

    fun salvar() {
        val estado = _uiState.value
        val peso = estado.peso.replace(",", ".").toDoubleOrNull()
        val altura = estado.altura.replace(",", ".").toDoubleOrNull()
        if (peso == null || altura == null || estado.dum.isBlank()) {
            _uiState.value = estado.copy(erro = "Preencha DUM, peso e altura corretamente.")
            return
        }
        if (altura > 3.0) {
            _uiState.value = estado.copy(erro = "Informe a altura em metros (ex: 1.65).")
            return
        }
        _uiState.value = estado.copy(salvando = true, erro = null, mensagemSucesso = null)
        viewModelScope.launch {
            _uiState.value = when (val resultado = repository.atualizar(estado.dum, peso, altura)) {
                is Resultado.Sucesso -> preencher(resultado.dados).copy(
                    salvando = false,
                    mensagemSucesso = "Dados atualizados. Idade gestacional: ${resultado.dados.semanas} semanas e ${resultado.dados.dias} dias."
                )
                is Resultado.Erro -> _uiState.value.copy(salvando = false, erro = resultado.mensagem)
            }
        }
    }

    fun desvincularMedico() {
        _uiState.value = _uiState.value.copy(salvando = true, erro = null, mensagemSucesso = null)
        viewModelScope.launch {
            when (val resultado = repository.desvincularMedico()) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(
                        salvando = false,
                        dados = _uiState.value.dados?.copy(medicoVinculado = false),
                        mensagemSucesso = "Médico desvinculado. Envie seu código a outro médico, se quiser."
                    )
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(salvando = false, erro = resultado.mensagem)
            }
        }
    }

    private fun preencher(dados: IdadeGestacionalResponse) = _uiState.value.copy(
        dados = dados,
        dum = dados.dataUltimaMenstruacao,
        peso = dados.pesoInicialKg.toString(),
        altura = dados.alturaMetros.toString()
    )
}
