package br.edu.ufmt.mothercare.app.ui.agendamento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ConsultaResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.MINIMO_CONSULTAS_PRE_NATAL
import br.edu.ufmt.mothercare.app.ui.components.dataHoraApiNoFuturo
import br.edu.ufmt.mothercare.app.ui.components.dataUtcDeMillis
import br.edu.ufmt.mothercare.app.ui.components.formatarDataApi
import br.edu.ufmt.mothercare.app.ui.components.formatarDataHoraApi
import br.edu.ufmt.mothercare.app.ui.components.formatarDataHoraBr
import br.edu.ufmt.mothercare.app.ui.components.hojeNoAparelho
import br.edu.ufmt.mothercare.app.ui.components.rotuloPeriodicidade
import br.edu.ufmt.mothercare.app.ui.components.rotuloStatusConsulta
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.BlueSurfaceTint
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import kotlinx.datetime.LocalDateTime

@Composable
fun AgendamentoScreen(
    gestanteId: String,
    agendamentoRepository: AgendamentoRepository
) {
    val viewModel: AgendamentoViewModel = viewModel(
        factory = GenericViewModelFactory { AgendamentoViewModel(gestanteId, agendamentoRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.carregar() }
    AgendamentoScreenContent(
        estado = estado,
        onDataSelecionada = viewModel::onDataSelecionada,
        onHorarioSelecionado = viewModel::onHorarioSelecionado,
        onSistolicaChange = viewModel::onSistolicaChange,
        onDiastolicaChange = viewModel::onDiastolicaChange,
        onConfirmarClick = viewModel::agendar,
        onCancelarConsulta = viewModel::cancelar,
        onTentarNovamente = viewModel::carregar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendamentoScreenContent(
    estado: AgendamentoUiState,
    onDataSelecionada: (String) -> Unit,
    onHorarioSelecionado: (String) -> Unit,
    onSistolicaChange: (String) -> Unit,
    onDiastolicaChange: (String) -> Unit,
    onConfirmarClick: () -> Unit,
    onCancelarConsulta: (ConsultaResponse) -> Unit = {},
    onTentarNovamente: (() -> Unit)? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Só hoje em diante (o DatePicker usa meia-noite UTC, mesma conversão da escolha abaixo).
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !dataUtcDeMillis(utcTimeMillis).let { it < hojeNoAparelho() }
        }
    )

    var consultaParaCancelar by remember { mutableStateOf<ConsultaResponse?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Agendar consulta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(8.dp))
        if (!estado.carregandoConsultas && estado.erroConsultas == null) {
            ContadorConsultas(estado.totalConsultasValidas)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // UC04 — check-in obrigatório: a PA é registrada junto com o agendamento.
        Text("1. Pressão arterial (obrigatória)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = estado.sistolica,
                onValueChange = onSistolicaChange,
                label = { Text("Sistólica (mmHg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = estado.diastolica,
                onValueChange = onDiastolicaChange,
                label = { Text("Diastólica (mmHg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // RF03 — escolha da data e dos horários livres da clínica.
        Text("2. Data e horário", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = if (estado.dataSelecionada.isBlank()) "" else formatarDataApi(estado.dataSelecionada),
            onValueChange = { },
            label = { Text("Data desejada") },
            placeholder = { Text("Toque para selecionar") },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth().clickable(enabled = !estado.bloqueadoPorRisco) { showDatePicker = true },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorariosLivres(estado, onHorarioSelecionado)
        Spacer(modifier = Modifier.height(16.dp))

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { onDataSelecionada(dataUtcDeMillis(it).toString()) }
                        showDatePicker = false
                    }) { Text("Escolher") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        estado.erro?.let {
            if (estado.bloqueadoPorRisco) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Agendamento bloqueado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
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
                    LocalDateTime.parse(resultado.dataHora).formatarDataHoraBr()
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

/** RF03: horários livres da data escolhida; sem horário, orienta a ligar para a clínica (A1). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun HorariosLivres(estado: AgendamentoUiState, onHorarioSelecionado: (String) -> Unit) {
    when {
        estado.dataSelecionada.isBlank() -> Text(
            "Escolha uma data para ver os horários livres.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        estado.carregandoHorarios -> LoadingIndicator()
        estado.mensagemSemHorarios != null -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(estado.mensagemSemHorarios, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
        else -> Column {
            Text("Horários livres", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                estado.horariosDisponiveis.forEach { horario ->
                    FilterChip(
                        selected = estado.horarioSelecionado == horario,
                        onClick = { onHorarioSelecionado(horario) },
                        label = { Text(horario) },
                        enabled = !estado.bloqueadoPorRisco
                    )
                }
            }
        }
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

@Preview()
@Composable
private fun AgendamentoPreviewVazio() {
    MotherCareTheme {
        AgendamentoScreenContent(AgendamentoUiState(carregandoConsultas = false), {}, {}, {}, {}, {})
    }
}

@Preview()
@Composable
private fun AgendamentoPreviewHorarios() {
    MotherCareTheme {
        AgendamentoScreenContent(
            AgendamentoUiState(
                dataSelecionada = "2026-10-15", horarioSelecionado = "09:30",
                sistolica = "118", diastolica = "76",
                horariosDisponiveis = listOf("08:00", "08:30", "09:30", "10:00"),
                carregandoConsultas = false, consultas = consultasExemplo
            ),
            {}, {}, {}, {}, {}
        )
    }
}

@Preview()
@Composable
private fun AgendamentoPreviewSemHorarios() {
    MotherCareTheme {
        AgendamentoScreenContent(
            AgendamentoUiState(
                dataSelecionada = "2026-10-17", sistolica = "118", diastolica = "76",
                mensagemSemHorarios = "Não há horários disponíveis em 2026-10-17. Ligue para a clínica para marcar sua consulta.",
                carregandoConsultas = false
            ),
            {}, {}, {}, {}, {}
        )
    }
}

@Preview()
@Composable
private fun AgendamentoPreviewSucesso() {
    MotherCareTheme {
        AgendamentoScreenContent(
            AgendamentoUiState(
                resultado = AgendamentoResponse(
                    id = "exemplo-id", dataHora = "2026-10-15T10:00:00",
                    semanaGestacionalNoAgendamento = 20, periodicidadeAplicada = "MENSAL",
                    totalConsultasRealizadasOuAgendadas = 2,
                    mensagem = "Consulta agendada. Total atual: 2 de 6 consultas mínimas recomendadas."
                ),
                carregandoConsultas = false
            ),
            {}, {}, {}, {}, {}
        )
    }
}

@Preview()
@Composable
private fun AgendamentoPreviewBloqueado() {
    MotherCareTheme {
        AgendamentoScreenContent(
            AgendamentoUiState(
                dataSelecionada = "2026-10-15", horarioSelecionado = "10:00",
                sistolica = "150", diastolica = "95",
                erro = "Agendamento eletivo bloqueado: a pressão arterial informada indicou risco de Síndrome Hipertensiva/Pré-eclâmpsia. Procure atendimento hospitalar imediato.",
                bloqueadoPorRisco = true, carregandoConsultas = false
            ),
            {}, {}, {}, {}, {}
        )
    }
}
