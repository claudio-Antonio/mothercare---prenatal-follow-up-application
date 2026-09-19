package br.edu.ufmt.mothercare.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.BlueSurfaceTint
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

@Composable
fun DashboardScreen(gestanteId: String, gestanteRepository: GestanteRepository) {
    val viewModel: DashboardViewModel = viewModel(
        factory = GenericViewModelFactory { DashboardViewModel(gestanteId, gestanteRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    DashboardScreenContent(estado)
}

@Composable
fun DashboardScreenContent(estado: DashboardUiState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Meu acompanhamento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            estado.carregando -> LoadingIndicator()
            estado.erro != null -> ErrorBanner(estado.erro)
            estado.dados != null -> {
                val dados = estado.dados
                Card(colors = CardDefaults.cardColors(containerColor = BlueSurfaceTint), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Idade Gestacional", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${dados.semanas} semanas e ${dados.dias} dias",
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BluePrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(onClick = {}, label = { Text(rotuloTrimestre(dados.trimestre)) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow("Data provável do parto", dados.dataProvavelParto)
                InfoRow("Data da última menstruação", dados.dataUltimaMenstruacao)
                InfoRow("IMC inicial", "%.1f".format(dados.imcInicial))
            }
        }
    }
}

@Composable
private fun InfoRow(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
    Divider()
}

private fun rotuloTrimestre(trimestre: String): String = when (trimestre) {
    "PRIMEIRO" -> "1º Trimestre"
    "SEGUNDO" -> "2º Trimestre"
    "TERCEIRO" -> "3º Trimestre"
    else -> trimestre
}

private val dadosExemplo = IdadeGestacionalResponse(
    gestanteId = "exemplo-id",
    dataUltimaMenstruacao = "2025-11-15",
    dataProvavelParto = "2026-08-22",
    semanas = 20,
    dias = 3,
    trimestre = "SEGUNDO",
    imcInicial = 22.0
)

@Preview(showBackground = true, name = "Dashboard - carregando")
@Composable
private fun DashboardPreviewCarregando() {
    MotherCareTheme { DashboardScreenContent(DashboardUiState(carregando = true)) }
}

@Preview(showBackground = true, name = "Dashboard - com dados")
@Composable
private fun DashboardPreviewComDados() {
    MotherCareTheme { DashboardScreenContent(DashboardUiState(carregando = false, dados = dadosExemplo)) }
}

@Preview(showBackground = true, name = "Dashboard - erro")
@Composable
private fun DashboardPreviewErro() {
    MotherCareTheme { DashboardScreenContent(DashboardUiState(carregando = false, erro = "Gestante não encontrada")) }
}
