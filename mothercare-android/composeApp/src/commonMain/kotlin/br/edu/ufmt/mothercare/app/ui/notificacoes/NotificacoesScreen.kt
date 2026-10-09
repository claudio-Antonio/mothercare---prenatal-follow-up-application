package br.edu.ufmt.mothercare.app.ui.notificacoes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.LembreteConsultaResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.NotificacaoResponse
import br.edu.ufmt.mothercare.app.data.repository.AgendamentoRepository
import br.edu.ufmt.mothercare.app.data.repository.NotificacaoRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.formatarDataHoraApi
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.BlueSurfaceTint
import br.edu.ufmt.mothercare.app.ui.theme.ErrorRed
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * RF08 + seção 8.3 — notificações in-app de gestante e médico. Os alertas críticos
 * chegam aqui (o push nativo depende de Firebase/APNs, não configurados neste projeto).
 */
@Composable
fun NotificacoesScreen(
    notificacaoRepository: NotificacaoRepository,
    agendamentoRepository: AgendamentoRepository? = null,
    gestanteId: String? = null
) {
    val viewModel: NotificacoesViewModel = viewModel(
        factory = GenericViewModelFactory { NotificacoesViewModel(notificacaoRepository, agendamentoRepository, gestanteId) }
    )
    val estado by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.carregar() }
    NotificacoesScreenContent(
        estado = estado,
        onMarcarLida = viewModel::marcarComoLida,
        onTentarNovamente = viewModel::carregar
    )
}

@Composable
fun NotificacoesScreenContent(
    estado: NotificacoesUiState,
    onMarcarLida: (NotificacaoResponse) -> Unit = {},
    onTentarNovamente: (() -> Unit)? = null
) {
    when {
        estado.carregando -> LoadingIndicator()
        estado.erro != null && estado.notificacoes.isEmpty() -> Column(modifier = Modifier.padding(20.dp)) {
            ErrorBanner(estado.erro, onTentarNovamente)
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Notificações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
                if (estado.naoLidas > 0) {
                    Text("${estado.naoLidas} não lida(s)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            estado.lembrete?.let { lembrete ->
                item { CartaoLembrete(lembrete) }
            }
            estado.erro?.let { erro -> item { ErrorBanner(erro) } }
            if (estado.notificacoes.isEmpty()) {
                item {
                    Text("Você não tem notificações.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(estado.notificacoes, key = { it.id }) { notificacao ->
                CartaoNotificacao(notificacao, onClick = { onMarcarLida(notificacao) })
            }
        }
    }
}

@Composable
private fun CartaoLembrete(lembrete: LembreteConsultaResponse) {
    Card(colors = CardDefaults.cardColors(containerColor = BlueSurfaceTint)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Lembrete de consulta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BluePrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(lembrete.mensagem.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CartaoNotificacao(notificacao: NotificacaoResponse, onClick: () -> Unit) {
    val critica = notificacao.tipo == "PRESSAO_CRITICA"
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !notificacao.lida, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                critica -> ErrorRed.copy(alpha = 0.12f)
                notificacao.lida -> MaterialTheme.colorScheme.surface
                else -> BlueSurfaceTint
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rotuloTipoNotificacao(notificacao.tipo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notificacao.lida) FontWeight.Normal else FontWeight.Bold,
                    color = if (critica) ErrorRed else BluePrimary,
                    modifier = Modifier.weight(1f)
                )
                if (!notificacao.lida) Text("nova", style = MaterialTheme.typography.labelSmall, color = BluePrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(notificacao.mensagem, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(formatarDataHoraApi(notificacao.criadaEm), style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun rotuloTipoNotificacao(tipo: String): String = when (tipo) {
    "PRESSAO_CRITICA" -> "Pressão arterial crítica"
    "ANEMIA" -> "Alerta de anemia"
    "DIABETES_GESTACIONAL" -> "Alerta de diabetes gestacional"
    "GANHO_PESO_EXCESSIVO" -> "Ganho de peso acima do esperado"
    "LEMBRETE_CONSULTA" -> "Lembrete de consulta"
    else -> tipo
}

@Preview()
@Composable
private fun NotificacoesPreview() {
    MotherCareTheme {
        NotificacoesScreenContent(
            NotificacoesUiState(
                carregando = false,
                lembrete = LembreteConsultaResponse(true, "Sua última consulta foi em 01/10/2026. Marque a próxima até 29/10/2026.", "2026-10-29"),
                notificacoes = listOf(
                    NotificacaoResponse("n1", "g1", "PRESSAO_CRITICA", "Sua pressão foi 150/95 mmHg. Procure atendimento hospitalar imediato.", false, "2026-10-07T13:00:00Z"),
                    NotificacaoResponse("n2", "g1", "ANEMIA", "Sua hemoglobina foi 10.5 g/dL, indicando possível anemia.", true, "2026-10-05T10:00:00Z")
                )
            )
        )
    }
}

@Preview()
@Composable
private fun NotificacoesVaziasPreview() {
    MotherCareTheme { NotificacoesScreenContent(NotificacoesUiState(carregando = false)) }
}
