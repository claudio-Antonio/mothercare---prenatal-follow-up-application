package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

/** UC03 + UC04: a PA entra no agendamento (check-in obrigatório). dataHoraDesejada = yyyy-MM-ddTHH:mm. */
@Serializable
data class AgendamentoRequest(
    val gestanteId: String,
    val dataHoraDesejada: String,
    val sistolica: Int,
    val diastolica: Int
)

@Serializable
data class AgendamentoResponse(
    val id: String,
    val dataHora: String,
    val semanaGestacionalNoAgendamento: Int,
    val periodicidadeAplicada: String,
    val totalConsultasRealizadasOuAgendadas: Long,
    val mensagem: String
)

/** status: AGENDADA | CONCLUIDA | CANCELADA */
@Serializable
data class ConsultaResponse(
    val id: String,
    val dataHora: String, // LocalDateTime ISO
    val status: String,
    val semanaGestacionalNoAgendamento: Int
)

/** RF03: horários livres da clínica na data (yyyy-MM-dd). horarios = "HH:mm". */
@Serializable
data class DisponibilidadeResponse(
    val data: String,
    val horarios: List<String> = emptyList(),
    val semHorarios: Boolean = false,
    val mensagem: String? = null
)

/** Seção 8.3: lembrete de próxima consulta quando o intervalo está expirando. */
@Serializable
data class LembreteConsultaResponse(
    val ativo: Boolean = false,
    val mensagem: String? = null,
    val prazoProximaConsulta: String? = null
)

@Serializable
data class ErroResponse(
    val timestamp: String? = null,
    val status: Int? = null,
    val mensagem: String? = null,
    val error: String? = null,
    val codigo: String? = null
)
