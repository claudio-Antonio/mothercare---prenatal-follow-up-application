package br.edu.ufmt.mothercare.app.ui.exame

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameHistoricoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameLaboratorialResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.TipoExame
import br.edu.ufmt.mothercare.app.data.repository.ExameRepository
import br.edu.ufmt.mothercare.app.data.repository.ProntuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExameUiState(
    val tipo: TipoExame = TipoExame.HEMOGLOBINA,
    val valorInput: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val resultado: ExameLaboratorialResponse? = null,
    /** Laudo opcional escolhido pela gestante; vai para o prontuário e é ligado ao exame (documentoId). */
    val laudoSelecionado: Uri? = null,
    val historico: List<ExameHistoricoResponse> = emptyList()
)

/**
 * Categoria do laudo = nome do item no checklist: assim o clinical-service grava o
 * tipo rastreado e o item do checklist do trimestre fica REALIZADO.
 */
fun categoriaDoLaudo(tipo: TipoExame): String = when (tipo) {
    TipoExame.HEMOGLOBINA -> "Hemograma completo"
    TipoExame.GLICEMIA_JEJUM -> "Glicemia de jejum"
}

/** UC08 - Emitir Alertas (RN03: Hb < 11 g/dL, Glicemia de jejum >= 92 mg/dL). */
class ExameViewModel(
    private val gestanteId: String,
    private val repository: ExameRepository,
    private val prontuarioRepository: ProntuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExameUiState())
    val uiState: StateFlow<ExameUiState> = _uiState.asStateFlow()

    init { carregarHistorico() }

    fun onTipoChange(tipo: TipoExame) { _uiState.value = _uiState.value.copy(tipo = tipo, resultado = null) }
    fun onValorChange(valor: String) { _uiState.value = _uiState.value.copy(valorInput = valor, resultado = null) }
    fun onLaudoSelecionado(uri: Uri?) { _uiState.value = _uiState.value.copy(laudoSelecionado = uri) }

    fun carregarHistorico() {
        viewModelScope.launch {
            val resultado = repository.historico(gestanteId)
            if (resultado is Resultado.Sucesso) _uiState.value = _uiState.value.copy(historico = resultado.dados)
        }
    }

    fun registrar() {
        val valor = _uiState.value.valorInput.replace(",", ".").toDoubleOrNull()
        if (valor == null || valor <= 0) {
            _uiState.value = _uiState.value.copy(erro = "Informe um valor numérico válido.")
            return
        }
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val estado = _uiState.value
            // 1) Laudo opcional: sobe primeiro para obter o documentoId.
            val documentoId = estado.laudoSelecionado?.let { uri ->
                when (val envio = prontuarioRepository.enviar(gestanteId, categoriaDoLaudo(estado.tipo), uri)) {
                    is Resultado.Sucesso -> envio.dados.id
                    is Resultado.Erro -> {
                        _uiState.value = _uiState.value.copy(carregando = false, erro = "Laudo não enviado: ${envio.mensagem}")
                        return@launch
                    }
                }
            }
            // 2) Valor numérico, ligado ao laudo quando houver.
            when (val resultado = repository.registrar(gestanteId, estado.tipo, valor, documentoId)) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(
                        carregando = false, resultado = resultado.dados, valorInput = "", laudoSelecionado = null
                    )
                    carregarHistorico()
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(carregando = false, erro = resultado.mensagem)
            }
        }
    }
}
