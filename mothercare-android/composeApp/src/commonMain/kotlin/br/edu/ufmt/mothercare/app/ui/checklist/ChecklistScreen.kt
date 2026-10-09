package br.edu.ufmt.mothercare.app.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
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
fun ChecklistScreen(
    gestanteId: String,
    checklistRepository: ChecklistRepository,
    aoEnviarLaudo: (nomeDoExame: String) -> Unit = {}
) {
    val viewModel: ChecklistViewModel = viewModel(
        factory = GenericViewModelFactory { ChecklistViewModel(gestanteId, checklistRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.carregar() }
    ChecklistScreenContent(estado, onTentarNovamente = viewModel::carregar, onItemClick = aoEnviarLaudo)
}

/** [onItemClick] != null: itens ainda não realizados abrem o envio do laudo (visão da gestante). */
@Composable
fun ChecklistScreenContent(
    estado: ChecklistUiState,
    onTentarNovamente: (() -> Unit)? = null,
    onItemClick: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Checklist de exames", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            estado.carregando -> LoadingIndicator()
            estado.erro != null -> ErrorBanner(estado.erro, onTentarNovamente)
            estado.dados != null && estado.dados.itens.isEmpty() ->
                Text("Nenhum exame previsto para este trimestre.", style = MaterialTheme.typography.bodyMedium)
            estado.dados != null -> {
                Text(rotuloTrimestre(estado.dados.trimestre), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (onItemClick != null) {
                    Text(
                        "Toque num exame pendente para enviar o laudo ao prontuário.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.dados.itens) { item ->
                        val clicavel = onItemClick != null && item.status != "REALIZADO"
                        Card(modifier = Modifier.fillMaxWidth().clickable(enabled = clicavel) { onItemClick?.invoke(item.nome) }) {
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

private fun rotuloTrimestre(trimestre: String): String = when (trimestre) {
    "PRIMEIRO" -> "Exames do 1º trimestre"
    "SEGUNDO" -> "Exames do 2º trimestre"
    "TERCEIRO" -> "Exames do 3º trimestre"
    else -> trimestre
}

@Composable
internal fun StatusBadge(status: String) {
    val (cor, texto) = when (status) {
        "REALIZADO" -> SuccessGreen to "Realizado"
        "PENDENTE" -> WarningAmber to "Pendente"
        else -> Color.Gray to "Sem laudo"
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

@Preview()
@Composable
private fun ChecklistPreviewCarregando() {
    MotherCareTheme { ChecklistScreenContent(ChecklistUiState(carregando = true)) }
}

@Preview()
@Composable
private fun ChecklistPreviewComDados() {
    MotherCareTheme { ChecklistScreenContent(ChecklistUiState(carregando = false, dados = checklistExemplo)) }
}
