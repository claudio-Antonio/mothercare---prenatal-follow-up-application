package br.edu.ufmt.mothercare.app.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.ChecklistExamesResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.ItemChecklistResponse
import br.edu.ufmt.mothercare.app.data.repository.ChecklistRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen
import br.edu.ufmt.mothercare.app.ui.theme.WarningAmber

@Composable
fun ChecklistScreen(gestanteId: String, checklistRepository: ChecklistRepository) {
    val viewModel: ChecklistViewModel = viewModel(
        factory = GenericViewModelFactory { ChecklistViewModel(gestanteId, checklistRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    ChecklistScreenContent(estado)
}

@Composable
fun ChecklistScreenContent(estado: ChecklistUiState) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Checklist de exames", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            estado.carregando -> LoadingIndicator()
            estado.erro != null -> ErrorBanner(estado.erro)
            estado.dados != null -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.dados.itens) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.nome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                StatusBadge(item.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (cor, texto) = when (status) {
        "REALIZADO" -> SuccessGreen to "Realizado"
        "PENDENTE" -> WarningAmber to "Pendente"
        else -> Color.Gray to "Não rastreado"
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(cor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(texto, color = cor, style = MaterialTheme.typography.labelLarge)
    }
}

private val checklistExemplo = ChecklistExamesResponse(
    gestanteId = "exemplo-id",
    trimestre = "PRIMEIRO",
    itens = listOf(
        ItemChecklistResponse("Hemograma completo", "REALIZADO"),
        ItemChecklistResponse("Glicemia de jejum", "PENDENTE"),
        ItemChecklistResponse("Sorologia para HIV", "NAO_RASTREADO"),
        ItemChecklistResponse("Ultrassonografia obstétrica inicial", "PENDENTE")
    )
)

@Preview(showBackground = true, name = "Checklist - carregando")
@Composable
private fun ChecklistPreviewCarregando() {
    MotherCareTheme { ChecklistScreenContent(ChecklistUiState(carregando = true)) }
}

@Preview(showBackground = true, name = "Checklist - com itens")
@Composable
private fun ChecklistPreviewComDados() {
    MotherCareTheme { ChecklistScreenContent(ChecklistUiState(carregando = false, dados = checklistExemplo)) }
}
