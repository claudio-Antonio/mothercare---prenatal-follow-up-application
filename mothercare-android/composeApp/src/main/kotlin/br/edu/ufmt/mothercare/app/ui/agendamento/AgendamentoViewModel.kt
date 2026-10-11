package br.edu.ufmt.mothercare.app.ui.agendamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgendamentoUiState(
    /** Data escolhida (yyyy-MM-dd). */
    val dataSelecionada: String = "",
    /** Horário escolhido entre os livres da clínica (HH:mm). */
    val horarioSelecionado: String = "",
    /** UC04 — check-in obrigatório: PA informada junto com o agendamento. */
    val sistolica: String = "",
    val diastolica: String = "",
    /** RF03 — horários livres da data escolhida. */
    val horariosDisponiveis: List<String> = emptyList(),
    val carregandoHorarios: Boolean = false,
    /** UC03 A1 — sem horários: orienta a gestante a ligar para a clínica. */
    val mensagemSemHorarios: String? = null,
    val carregando: Boolean = false,
    val erro: String? = null,
    /** RF05 — PA crítica: bloqueio do agendamento eletivo (procurar atendimento). */
    val bloqueadoPorRisco: Boolean = false,
    val resultado: AgendamentoResponse? = null,
    val carregandoConsultas: Boolean = true,
    val erroConsultas: String? = null,
    val consultas: List<ConsultaResponse> = emptyList(),
    val cancelandoId: String? = null
) {
    /** Consultas que contam para o mínimo de 6 do pré-natal (canceladas não contam). */
    val totalConsultasValidas: Int get() = consultas.count { it.status != "CANCELADA" }

    /** Valor enviado ao backend (LocalDateTime sem fuso). */
    val dataHora: String
        get() = if (dataSelecionada.isBlank() || horarioSelecionado.isBlank()) "" else "${dataSelecionada}T$horarioSelecionado:00"
}

class AgendamentoViewModel(
    private val gestanteId: String,
    private val repository: AgendamentoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgendamentoUiState())
    val uiState: StateFlow<AgendamentoUiState> = _uiState.asStateFlow()

    /** Chamado ao abrir a aba: lista as consultas. A PA é informada no próprio agendamento. */
    fun carregar() {
        _uiState.value = _uiState.value.copy(carregandoConsultas = true, erroConsultas = null)
        viewModelScope.launch {
            _uiState.value = when (val consultas = repository.listar(gestanteId)) {
                is Resultado.Sucesso -> _uiState.value.copy(carregandoConsultas = false, consultas = consultas.dados)
                is Resultado.Erro -> _uiState.value.copy(carregandoConsultas = false, erroConsultas = consultas.mensagem)
            }
        }
    }

    /** RF03: ao escolher a data, busca os horários livres da clínica. */
    fun onDataSelecionada(data: String) {
        _uiState.value = _uiState.value.copy(
            dataSelecionada = data, horarioSelecionado = "", horariosDisponiveis = emptyList(),
            mensagemSemHorarios = null, resultado = null, erro = null
        )
        carregarHorarios(data)
    }

    private fun carregarHorarios(data: String) {
        _uiState.value = _uiState.value.copy(carregandoHorarios = true)
        viewModelScope.launch {
            val estadoAtual = _uiState.value
            if (estadoAtual.dataSelecionada != data) return@launch
            _uiState.value = when (val disponibilidade = repository.disponibilidade(data)) {
                is Resultado.Sucesso -> _uiState.value.copy(
                    carregandoHorarios = false,
                    horariosDisponiveis = disponibilidade.dados.horarios,
                    mensagemSemHorarios = disponibilidade.dados.mensagem.takeIf { disponibilidade.dados.semHorarios }
                )
                is Resultado.Erro -> _uiState.value.copy(carregandoHorarios = false, erro = disponibilidade.mensagem)
            }
        }
    }

    fun onHorarioSelecionado(horario: String) {
        _uiState.value = _uiState.value.copy(horarioSelecionado = horario, resultado = null, erro = null)
    }

    fun onSistolicaChange(valor: String) = onPressaoChange { copy(sistolica = valor.filter(Char::isDigit).take(3)) }

    fun onDiastolicaChange(valor: String) = onPressaoChange { copy(diastolica = valor.filter(Char::isDigit).take(3)) }

    /** Editar a PA libera um novo envio (o bloqueio anterior vale para os valores antigos). */
    private fun onPressaoChange(alteracao: AgendamentoUiState.() -> AgendamentoUiState) {
        _uiState.value = _uiState.value.alteracao().copy(resultado = null, erro = null, bloqueadoPorRisco = false)
    }

    fun agendar() {
        val estado = _uiState.value
        val sistolica = estado.sistolica.toIntOrNull()
        val diastolica = estado.diastolica.toIntOrNull()
        when {
            estado.dataHora.isBlank() -> _uiState.value = estado.copy(erro = "Escolha a data e um horário livre.")
            sistolica == null || diastolica == null ->
                _uiState.value = estado.copy(erro = "Informe a pressão arterial (sistólica e diastólica) para o check-in obrigatório.")
            else -> {
                _uiState.value = estado.copy(carregando = true, erro = null, bloqueadoPorRisco = false)
                viewModelScope.launch {
                    when (val resultado = repository.agendar(gestanteId, estado.dataHora, sistolica, diastolica)) {
                        is Resultado.Sucesso -> {
                            _uiState.value = _uiState.value.copy(
                                carregando = false, resultado = resultado.dados,
                                dataSelecionada = "", horarioSelecionado = "", horariosDisponiveis = emptyList(),
                                sistolica = "", diastolica = ""
                            )
                            carregar()
                        }
                        is Resultado.Erro -> _uiState.value = _uiState.value.copy(
                            carregando = false,
                            erro = resultado.mensagem,
                            // Só a PA crítica (RF05) bloqueia; intervalo, horário ocupado e DPP são erros comuns.
                            bloqueadoPorRisco = resultado.codigo == CODIGO_RISCO_PA
                        )
                    }
                    if (_uiState.value.dataSelecionada.isNotBlank()) carregarHorarios(_uiState.value.dataSelecionada)
                }
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
