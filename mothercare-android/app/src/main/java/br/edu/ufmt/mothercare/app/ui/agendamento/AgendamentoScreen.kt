package br.edu.ufmt.mothercare.app.ui.agendamento

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun AgendamentoScreenContent(
    estado: AgendamentoUiState,
    onDataHoraChange: (String) -> Unit,
    onConfirmarClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Agendar consulta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = estado.dataHora, onValueChange = onDataHoraChange,
            label = { Text("Data e hora desejadas") },
            placeholder = { Text("yyyy-MM-ddTHH:mm:ss, ex: 2026-10-15T10:00:00") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

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
            Spacer(modifier = Modifier.height(20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = BlueSurfaceTint)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Consulta agendada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Data: ${resultado.dataHora}")
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
