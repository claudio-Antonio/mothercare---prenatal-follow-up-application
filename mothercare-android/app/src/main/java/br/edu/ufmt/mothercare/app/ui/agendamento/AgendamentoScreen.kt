package br.edu.ufmt.mothercare.app.ui.agendamento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import br.edu.ufmt.mothercare.app.data.repository.CheckInRepository
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.MINIMO_CONSULTAS_PRE_NATAL
import br.edu.ufmt.mothercare.app.ui.components.dataHoraApiNoFuturo
import br.edu.ufmt.mothercare.app.ui.components.formatarDataHoraApi
import br.edu.ufmt.mothercare.app.ui.components.rotuloPeriodicidade
import br.edu.ufmt.mothercare.app.ui.components.rotuloStatusConsulta
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.BlueSurfaceTint
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AgendamentoScreen(
    gestanteId: String,
    agendamentoRepository: AgendamentoRepository,
    checkInRepository: CheckInRepository,
    aoIrParaCheckIn: () -> Unit = {}
) {
    val viewModel: AgendamentoViewModel = viewModel(
        factory = GenericViewModelFactory { AgendamentoViewModel(gestanteId, agendamentoRepository, checkInRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    // Recarrega ao abrir a aba: um check-in feito na aba ao lado muda o bloqueio.
    LaunchedEffect(Unit) { viewModel.carregar() }
    AgendamentoScreenContent(
        estado = estado,
        onDataHoraChange = viewModel::onDataHoraChange,
        onConfirmarClick = viewModel::agendar,
        onCancelarConsulta = viewModel::cancelar,
        onTentarNovamente = viewModel::carregar,
        onIrParaCheckIn = aoIrParaCheckIn
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendamentoScreenContent(
    estado: AgendamentoUiState,
    onDataHoraChange: (String) -> Unit,
    onConfirmarClick: () -> Unit,
    onCancelarConsulta: (ConsultaResponse) -> Unit = {},
    onTentarNovamente: (() -> Unit)? = null,
    onIrParaCheckIn: () -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Só hoje em diante (o DatePicker usa meia-noite UTC, mesma conversão do confirmar abaixo).
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.of("UTC")).toLocalDate().isBefore(LocalDate.now())
        }
    )
    val timePickerState = rememberTimePickerState()

    // Formatação amigável para exibição no campo
    val dataHoraExibicao = remember(estado.dataHora) {
        try {
            if (estado.dataHora.isNotBlank()) {
                val dt = LocalDateTime.parse(estado.dataHora)
                dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))
            } else ""
        } catch (e: Exception) {
            estado.dataHora
        }
    }

    var consultaParaCancelar by remember { mutableStateOf<ConsultaResponse?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Agendar consulta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(8.dp))
        if (!estado.carregandoConsultas && estado.erroConsultas == null) {
            ContadorConsultas(estado.totalConsultasValidas)
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = dataHoraExibicao,
            onValueChange = { },
            label = { Text("Data e hora desejadas") },
            placeholder = { Text("Toque para selecionar") },
            readOnly = true,
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !estado.bloqueadoPorRisco) { showDatePicker = true },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Diálogos de Seleção ---

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        showTimePicker = true
                    }) { Text("Próximo") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val data = datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                        } ?: LocalDateTime.now().toLocalDate()

                        val hora = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val isoString = LocalDateTime.of(data, hora).toString()

                        onDataHoraChange(isoString)
                        showTimePicker = false
                    }) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Voltar") }
                },
                title = { Text("Selecione o horário") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                }
            )
        }

        estado.erro?.let {
            if (estado.bloqueadoPorRisco) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Agendamento bloqueado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onIrParaCheckIn, contentPadding = PaddingValues(0.dp)) { Text("Fazer novo check-in") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                ErrorBanner(it)
            }
        }

        if (!estado.bloqueadoPorRisco) {
            PrimaryButton(texto = "Confirmar agendamento", carregando = estado.carregando) { onConfirmarClick() }
        }

        estado.resultado?.let { resultado ->
            val dataHoraFormatada = remember(resultado.dataHora) {
                try {
                    val dt = LocalDateTime.parse(resultado.dataHora)
                    dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))
                } catch (e: Exception) {
                    resultado.dataHora
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = BlueSurfaceTint)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Consulta agendada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Data: $dataHoraFormatada")
                    Text("Periodicidade: ${rotuloPeriodicidade(resultado.periodicidadeAplicada)}")
                    Text("Semana gestacional: ${resultado.semanaGestacionalNoAgendamento}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(resultado.mensagem, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Minhas consultas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        when {
            estado.carregandoConsultas -> LoadingIndicator()
            estado.erroConsultas != null -> ErrorBanner(estado.erroConsultas, onTentarNovamente)
            estado.consultas.isEmpty() -> Text("Nenhuma consulta agendada ainda.", style = MaterialTheme.typography.bodyMedium)
            else -> estado.consultas.forEach { consulta ->
                LinhaConsulta(
                    consulta = consulta,
                    cancelando = estado.cancelandoId == consulta.id,
                    onCancelar = { consultaParaCancelar = consulta }
                )
            }
        }
    }

    consultaParaCancelar?.let { consulta ->
        AlertDialog(
            onDismissRequest = { consultaParaCancelar = null },
            title = { Text("Cancelar consulta?") },
            text = { Text("A consulta de ${formatarDataHoraApi(consulta.dataHora)} será cancelada.") },
            confirmButton = {
                TextButton(onClick = { onCancelarConsulta(consulta); consultaParaCancelar = null }) { Text("Cancelar consulta") }
            },
            dismissButton = { TextButton(onClick = { consultaParaCancelar = null }) { Text("Voltar") } }
        )
    }
}

/** Documento de regras de negócio: mínimo de 6 consultas no pré-natal. */
@Composable
private fun ContadorConsultas(total: Int) {
    val atingido = total >= MINIMO_CONSULTAS_PRE_NATAL
    Column {
        Text(
            if (atingido) "Meta mínima de $MINIMO_CONSULTAS_PRE_NATAL consultas atingida ✓"
            else "$total de $MINIMO_CONSULTAS_PRE_NATAL consultas mínimas recomendadas",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (total.toFloat() / MINIMO_CONSULTAS_PRE_NATAL).coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Também usada (sem cancelar) no detalhe da gestante do painel do médico. */
@Composable
internal fun LinhaConsulta(consulta: ConsultaResponse, cancelando: Boolean = false, onCancelar: (() -> Unit)? = null) {
    val cancelavel = consulta.status == "AGENDADA" && dataHoraApiNoFuturo(consulta.dataHora)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formatarDataHoraApi(consulta.dataHora), fontWeight = FontWeight.SemiBold)
            Text(
                "${rotuloStatusConsulta(consulta.status)} · ${consulta.semanaGestacionalNoAgendamento}ª semana",
                style = MaterialTheme.typography.bodyMedium,
                color = if (consulta.status == "CANCELADA") MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
        }
        when {
            cancelando -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            cancelavel && onCancelar != null -> TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    }
    HorizontalDivider()
}

private val consultasExemplo = listOf(
    ConsultaResponse("c1", "2026-11-10T09:00:00", "AGENDADA", 24),
    ConsultaResponse("c2", "2026-10-01T14:30:00", "CONCLUIDA", 19),
    ConsultaResponse("c3", "2026-09-20T08:00:00", "CANCELADA", 18)
)

@Preview(showBackground = true, name = "Agendamento - vazio")
@Composable
private fun AgendamentoPreviewVazio() {
    MotherCareTheme { AgendamentoScreenContent(AgendamentoUiState(carregandoConsultas = false), {}, {}) }
}

@Preview(showBackground = true, name = "Agendamento - com consultas")
@Composable
private fun AgendamentoPreviewConsultas() {
    MotherCareTheme { AgendamentoScreenContent(AgendamentoUiState(carregandoConsultas = false, consultas = consultasExemplo), {}, {}) }
}

@Preview(showBackground = true, name = "Agendamento - sucesso")
@Composable
private fun AgendamentoPreviewSucesso() {
    MotherCareTheme {
        AgendamentoScreenContent(
            AgendamentoUiState(
                dataHora = "2026-10-15T10:00:00",
                resultado = AgendamentoResponse(
                    id = "exemplo-id", dataHora = "2026-10-15T10:00:00",
                    semanaGestacionalNoAgendamento = 20, periodicidadeAplicada = "MENSAL",
                    totalConsultasRealizadasOuAgendadas = 2,
                    mensagem = "Consulta agendada. Total atual: 2 de 6 consultas mínimas recomendadas."
                )
            ),
            {}, {}
        )
    }
}

@Preview(showBackground = true, name = "Agendamento - bloqueado por risco")
@Composable
private fun AgendamentoPreviewBloqueado() {
    MotherCareTheme {
        AgendamentoScreenContent(
            AgendamentoUiState(
                dataHora = "2026-10-15T10:00:00",
                erro = "Agendamento eletivo bloqueado: o último check-in de pressão arterial indicou risco de Síndrome Hipertensiva/Pré-eclâmpsia. Procure atendimento hospitalar imediato.",
                bloqueadoPorRisco = true
            ),
            {}, {}
        )
    }
}
