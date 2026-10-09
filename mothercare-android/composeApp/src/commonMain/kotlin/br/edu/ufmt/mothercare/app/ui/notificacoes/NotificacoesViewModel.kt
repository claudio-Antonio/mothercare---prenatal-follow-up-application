package br.edu.ufmt.mothercare.app.ui.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.LembreteConsultaResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.NotificacaoResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import br.edu.ufmt.mothercare.app.data.repository.NotificacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificacoesUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val notificacoes: List<NotificacaoResponse> = emptyList(),
    /** Seção 8.3 — só para a gestante: lembrete de próxima consulta quando o intervalo está expirando. */
    val lembrete: LembreteConsultaResponse? = null
) {
    val naoLidas: Int get() = notificacoes.count { !it.lida }
}

/**
 * RF08 — caixa de notificações. [gestanteId] é null para o médico: o painel dele não tem
 * lembrete de consulta (o lembrete é da gestante).
 */
class NotificacoesViewModel(
    private val notificacaoRepository: NotificacaoRepository,
    private val agendamentoRepository: AgendamentoRepository?,
    private val gestanteId: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificacoesUiState())
    val uiState: StateFlow<NotificacoesUiState> = _uiState.asStateFlow()

    fun carregar() {
        _uiState.value = _uiState.value.copy(carregando = true, erro = null)
        viewModelScope.launch {
            val lembrete = gestanteId?.let { agendamentoRepository?.lembrete(it) }
            _uiState.value = when (val lista = notificacaoRepository.listar()) {
                is Resultado.Sucesso -> _uiState.value.copy(
                    carregando = false,
                    notificacoes = lista.dados,
                    lembrete = (lembrete as? Resultado.Sucesso)?.dados?.takeIf { it.ativo }
                )
                is Resultado.Erro -> _uiState.value.copy(carregando = false, erro = lista.mensagem)
            }
        }
    }

    fun marcarComoLida(notificacao: NotificacaoResponse) {
        if (notificacao.lida) return
        viewModelScope.launch {
            when (val resultado = notificacaoRepository.marcarComoLida(notificacao.id)) {
                is Resultado.Sucesso -> _uiState.value = _uiState.value.copy(
                    notificacoes = _uiState.value.notificacoes.map { if (it.id == notificacao.id) resultado.dados else it }
                )
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(erro = resultado.mensagem)
            }
        }
    }
}
