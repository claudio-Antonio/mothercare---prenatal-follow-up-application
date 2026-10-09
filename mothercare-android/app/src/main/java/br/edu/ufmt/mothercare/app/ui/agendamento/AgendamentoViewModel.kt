package br.edu.ufmt.mothercare.app.ui.agendamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import br.edu.ufmt.mothercare.app.data.repository.CheckInRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class AgendamentoUiState(
    val dataHora: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val bloqueadoPorRisco: Boolean = false,
    val resultado: AgendamentoResponse? = null,
    val carregandoConsultas: Boolean = true,
    val erroConsultas: String? = null,
    val consultas: List<ConsultaResponse> = emptyList(),
    val cancelandoId: String? = null
) {
    /** Consultas que contam para o mínimo de 6 do pré-natal (canceladas não contam). */
    val totalConsultasValidas: Int get() = consultas.count { it.status != "CANCELADA" }
}

class AgendamentoViewModel(
    private val gestanteId: String,
    private val repository: AgendamentoRepository,
    private val checkInRepository: CheckInRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgendamentoUiState())
    val uiState: StateFlow<AgendamentoUiState> = _uiState.asStateFlow()

    fun onDataHoraChange(valor: String) { _uiState.value = _uiState.value.copy(dataHora = valor, resultado = null) }

    /**
     * Chamado sempre que a aba abre: consulta o último check-in (RF05) para
     * mostrar o bloqueio ANTES de a gestante tentar agendar, e lista as consultas.
     */
    fun carregar() {
        _uiState.value = _uiState.value.copy(carregandoConsultas = true, erroConsultas = null)
        viewModelScope.launch {
            val pressao = async { checkInRepository.ultimoStatus(gestanteId) }
            val consultas = repository.listar(gestanteId)
            val critico = (pressao.await() as? Resultado.Sucesso)?.dados?.nivelRisco == CODIGO_NIVEL_CRITICO
            _uiState.value = when (consultas) {
                is Resultado.Sucesso -> _uiState.value.copy(carregandoConsultas = false, consultas = consultas.dados)
                is Resultado.Erro -> _uiState.value.copy(carregandoConsultas = false, erroConsultas = consultas.mensagem)
            }.let { estado ->
                if (critico) estado.copy(bloqueadoPorRisco = true, erro = MENSAGEM_BLOQUEIO)
                else if (estado.bloqueadoPorRisco) estado.copy(bloqueadoPorRisco = false, erro = null)
                else estado
            }
        }
    }

    fun agendar() {
        if (_uiState.value.dataHora.isBlank()) {
            _uiState.value = _uiState.value.copy(erro = "Informe a data e hora desejadas.")
            return
        }
        val noFuturo = runCatching { LocalDateTime.parse(_uiState.value.dataHora).isAfter(LocalDateTime.now()) }.getOrDefault(true)
        if (!noFuturo) {
            _uiState.value = _uiState.value.copy(erro = "Escolha um horário no futuro.", bloqueadoPorRisco = false)
            return
        }
        _uiState.value = _uiState.value.copy(carregando = true, erro = null, bloqueadoPorRisco = false)
        viewModelScope.launch {
            when (val resultado = repository.agendar(gestanteId, _uiState.value.dataHora.trim())) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(carregando = false, resultado = resultado.dados, dataHora = "")
                    carregar()
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(
                    // Só o 409 de PA crítica (RF05) é "bloqueio por risco"; o 409 de intervalo
                    // mínimo (RN04) é erro comum — a gestante só precisa escolher outra data.
                    carregando = false, erro = resultado.mensagem, bloqueadoPorRisco = resultado.codigo == CODIGO_RISCO_PA
                )
            }
        }
    }

    fun cancelar(consulta: ConsultaResponse) {
        _uiState.value = _uiState.value.copy(cancelandoId = consulta.id, erroConsultas = null)
        viewModelScope.launch {
            when (val resultado = repository.cancelar(consulta.id)) {
                is Resultado.Sucesso -> {
                    _uiState.value = _uiState.value.copy(cancelandoId = null)
                    carregar()
                }
                is Resultado.Erro -> _uiState.value = _uiState.value.copy(cancelandoId = null, erroConsultas = resultado.mensagem)
            }
        }
    }
}

private const val CODIGO_RISCO_PA = "RISCO_PA"
private const val CODIGO_NIVEL_CRITICO = "CRITICO"
private const val MENSAGEM_BLOQUEIO =
    "Seu último check-in de pressão arterial indicou risco. Procure atendimento hospitalar; " +
        "o agendamento eletivo volta a ser liberado após um novo check-in com pressão normal."
