package br.edu.ufmt.mothercare.app.ui.medico

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.GestanteResumoResponse
import br.edu.ufmt.mothercare.app.data.repository.MedicoRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.rotuloRisco
import br.edu.ufmt.mothercare.app.ui.theme.ErrorRed
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen

/** UC07 - Painel do médico: lista as gestantes vinculadas, alertas em destaque. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicoDashboardScreen(
    medicoRepository: MedicoRepository,
    aoSair: () -> Unit,
    nome: String? = null,
    aoAbrirGestante: (gestanteId: String, nome: String?) -> Unit = { _, _ -> },
    aoAbrirNotificacoes: () -> Unit = {}
) {
    val viewModel: MedicoDashboardViewModel = viewModel(
        factory = GenericViewModelFactory { MedicoDashboardViewModel(medicoRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    // Recarrega ao voltar do detalhe: os alertas podem ter mudado.
    LaunchedEffect(Unit) { viewModel.carregar() }
    var mostrarVinculo by remember { mutableStateOf(false) }

    LaunchedEffect(estado.vinculoConcluido) {
        if (estado.vinculoConcluido) {
            mostrarVinculo = false
            viewModel.vinculoConsumido()
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarVinculo = true },
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                text = { Text("Vincular gestante") }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(nome?.let { "Minhas gestantes · ${it.substringBefore(' ')}" } ?: "Minhas gestantes") },
                actions = {
                    IconButton(onClick = aoAbrirNotificacoes) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notificações")
                    }
                    IconButton(onClick = aoSair) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            MedicoDashboardScreenContent(estado, onTentarNovamente = viewModel::carregar, onGestanteClick = aoAbrirGestante)
        }
    }

    if (mostrarVinculo) {
        DialogoVinculo(
            vinculando = estado.vinculando,
            erro = estado.erroVinculo,
            onConfirmar = viewModel::vincular,
            onCancelar = { mostrarVinculo = false; viewModel.vinculoConsumido() }
        )
    }
}

@Composable
private fun DialogoVinculo(vinculando: Boolean, erro: String?, onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Vincular gestante") },
        text = {
            Column {
                Text("Peça à gestante o código de acompanhamento exibido na tela inicial do app dela.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = codigo, onValueChange = { codigo = it }, label = { Text("Código") }, singleLine = true)
                erro?.let { ErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(codigo) }, enabled = !vinculando) { Text(if (vinculando) "Vinculando…" else "Vincular") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
fun MedicoDashboardScreenContent(
    estado: MedicoDashboardUiState,
    onTentarNovamente: (() -> Unit)? = null,
    onGestanteClick: (gestanteId: String, nome: String?) -> Unit = { _, _ -> }
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        when {
            estado.carregando -> LoadingIndicator()
            estado.erro != null -> ErrorBanner(estado.erro, onTentarNovamente)
            estado.gestantes.isEmpty() -> Text(
                "Nenhuma gestante vinculada a você ainda. Use \"Vincular gestante\" com o código que ela vê no app.",
                style = MaterialTheme.typography.bodyMedium
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(estado.gestantes) { gestante ->
                    GestanteResumoCard(gestante, onClick = { onGestanteClick(gestante.gestanteId, gestante.nomeGestante) })
                }
            }
        }
    }
}

@Composable
private fun GestanteResumoCard(gestante: GestanteResumoResponse, onClick: () -> Unit = {}) {
    val emAlerta = gestante.ultimoNivelRiscoPA == "CRITICO" || gestante.possuiAlertaLaboratorial
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (emAlerta) ErrorRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = gestante.nomeGestante ?: "Gestante sem nome disponível",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ID: ${gestante.gestanteId}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text("${gestante.semanas} semanas de gestação", style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(texto = "PA ${rotuloRisco(gestante.ultimoNivelRiscoPA).lowercase()}", critico = gestante.ultimoNivelRiscoPA == "CRITICO")
                if (gestante.possuiAlertaLaboratorial) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(texto = "Exame alterado", critico = true)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(texto: String, critico: Boolean) {
    val cor = if (critico) ErrorRed else SuccessGreen
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(cor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(texto, color = cor, style = MaterialTheme.typography.labelLarge)
    }
}

private val gestantesExemplo = listOf(
    GestanteResumoResponse(
        gestanteId = "a1b2c3d4-1111-4e5f-8a9b-000000000001",
        nomeGestante = "Maria Silva",
        semanas = 32L, ultimoNivelRiscoPA = "CRITICO", possuiAlertaLaboratorial = true
    ),
    GestanteResumoResponse(
        gestanteId = "e5f6a7b8-2222-4e5f-8a9b-000000000002",
        nomeGestante = "Ana Souza",
        semanas = 20L, ultimoNivelRiscoPA = "NORMAL", possuiAlertaLaboratorial = false
    ),
    GestanteResumoResponse(
        gestanteId = "c9d0e1f2-3333-4e5f-8a9b-000000000003",
        nomeGestante = null,
        semanas = 12L, ultimoNivelRiscoPA = "NORMAL", possuiAlertaLaboratorial = false
    )
)

@Preview()
@Composable
private fun MedicoDashboardPreviewCarregando() {
    MotherCareTheme { MedicoDashboardScreenContent(MedicoDashboardUiState(carregando = true)) }
}

@Preview()
@Composable
private fun MedicoDashboardPreviewComDados() {
    MotherCareTheme { MedicoDashboardScreenContent(MedicoDashboardUiState(carregando = false, gestantes = gestantesExemplo)) }
}

@Preview()
@Composable
private fun MedicoDashboardPreviewVazio() {
    MotherCareTheme { MedicoDashboardScreenContent(MedicoDashboardUiState(carregando = false, gestantes = emptyList())) }
}
