package br.edu.ufmt.mothercare.app.ui.agendamento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.AgendamentoResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.BlueSurfaceTint
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AgendamentoScreen(gestanteId: String, agendamentoRepository: AgendamentoRepository) {
    val viewModel: AgendamentoViewModel = viewModel(
        factory = GenericViewModelFactory { AgendamentoViewModel(gestanteId, agendamentoRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    AgendamentoScreenContent(
        estado = estado,
        onDataHoraChange = viewModel::onDataHoraChange,
        onConfirmarClick = viewModel::agendar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendamentoScreenContent(
    estado: AgendamentoUiState,
    onDataHoraChange: (String) -> Unit,
    onConfirmarClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
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

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Agendar consulta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dataHoraExibicao,
            onValueChange = { },
            label = { Text("Data e hora desejadas") },
            placeholder = { Text("Toque para selecionar") },
            readOnly = true,
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
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
                    }
                }
            } else {
                ErrorBanner(it)
            }
        }

        PrimaryButton(texto = "Confirmar agendamento", carregando = estado.carregando) { onConfirmarClick() }

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
                    Text("Periodicidade: ${resultado.periodicidadeAplicada}")
                    Text("Semana gestacional: ${resultado.semanaGestacionalNoAgendamento}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(resultado.mensagem, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Agendamento - vazio")
@Composable
private fun AgendamentoPreviewVazio() {
    MotherCareTheme { AgendamentoScreenContent(AgendamentoUiState(), {}, {}) }
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
