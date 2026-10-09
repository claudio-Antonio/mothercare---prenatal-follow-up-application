package br.edu.ufmt.mothercare.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.UltimoStatusPressaoResponse
import br.edu.ufmt.mothercare.app.data.repository.CheckInRepository
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.formatarDataApi
import br.edu.ufmt.mothercare.app.ui.components.formatarDataHoraApi
import br.edu.ufmt.mothercare.app.ui.components.rotuloTrimestre
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.BlueSurfaceTint
import br.edu.ufmt.mothercare.app.ui.theme.ErrorRed
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen

@Composable
fun DashboardScreen(
    gestanteId: String,
    gestanteRepository: GestanteRepository,
    checkInRepository: CheckInRepository,
    nome: String?,
    aoIrParaCheckIn: () -> Unit = {}
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = GenericViewModelFactory { DashboardViewModel(gestanteId, gestanteRepository, checkInRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    // Recarrega ao voltar para a aba: o status de PA muda depois de um check-in.
    LaunchedEffect(Unit) { viewModel.carregar() }
    DashboardScreenContent(estado, nome = nome, onTentarNovamente = viewModel::carregar, onFazerCheckIn = aoIrParaCheckIn)
}

@Composable
fun DashboardScreenContent(
    estado: DashboardUiState,
    nome: String? = null,
    onTentarNovamente: (() -> Unit)? = null,
    onFazerCheckIn: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text(
            if (nome.isNullOrBlank()) "Meu acompanhamento" else "Olá, ${nome.substringBefore(' ')}",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            estado.carregando -> LoadingIndicator()
            estado.erro != null -> ErrorBanner(estado.erro, onTentarNovamente)
            estado.dados != null -> {
                val dados = estado.dados
                Card(colors = CardDefaults.cardColors(containerColor = BlueSurfaceTint), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Idade Gestacional", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${dados.semanas} semanas e ${dados.dias} dias",
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BluePrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(onClick = {}, label = { Text(rotuloTrimestre(dados.trimestre)) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                estado.statusPressao?.let { StatusPressaoCard(it, onFazerCheckIn) }
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Data provável do parto", formatarDataApi(dados.dataProvavelParto))
                InfoRow("Data da última menstruação", formatarDataApi(dados.dataUltimaMenstruacao))
                InfoRow("IMC inicial", "%.1f".format(dados.imcInicial))
                Spacer(modifier = Modifier.height(16.dp))
                if (dados.medicoVinculado) {
                    Text("✓ Seu médico acompanha seus dados pelo painel.", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
                } else {
                    CodigoAcompanhamento(dados.gestanteId)
                }
            }
        }
    }
}

/** RF05 na tela inicial: a gestante vê o bloqueio antes de tentar agendar. */
@Composable
private fun StatusPressaoCard(status: UltimoStatusPressaoResponse, onFazerCheckIn: () -> Unit) {
    val critico = status.nivelRisco == "CRITICO"
    val semRegistro = status.registradoEm == null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (critico) ErrorRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                when {
                    semRegistro -> "Pressão arterial: nenhum check-in ainda"
                    critico -> "⚠ Última pressão arterial: crítica"
                    else -> "Última pressão arterial: normal"
                },
                fontWeight = FontWeight.SemiBold,
                color = if (critico) ErrorRed else MaterialTheme.colorScheme.onSurface
            )
            status.registradoEm?.let { Text("Registrada em ${formatarDataHoraApi(it)}", style = MaterialTheme.typography.bodyMedium) }
            if (critico) {
                Text(
                    "Procure atendimento hospitalar. O agendamento de consultas fica bloqueado até um novo check-in normal.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onFazerCheckIn, contentPadding = PaddingValues(0.dp)) {
                Text(if (semRegistro) "Fazer check-in" else "Novo check-in")
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
    HorizontalDivider()
}

/** Código que a gestante passa ao médico para ele vinculá-la no painel (POST /gestantes/medico/vincular). */
@Composable
private fun CodigoAcompanhamento(gestanteId: String) {
    val clipboard = LocalClipboardManager.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Código de acompanhamento", fontWeight = FontWeight.SemiBold)
            Text(
                "Envie este código ao seu médico para ele acompanhar seus dados no painel.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(gestanteId, style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { clipboard.setText(AnnotatedString(gestanteId)) }) { Text("Copiar código") }
        }
    }
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
    MotherCareTheme {
        DashboardScreenContent(
            DashboardUiState(
                carregando = false, dados = dadosExemplo,
                statusPressao = UltimoStatusPressaoResponse("exemplo-id", "NORMAL", "2026-10-01T13:00:00Z")
            ),
            nome = "Maria Silva"
        )
    }
}

@Preview(showBackground = true, name = "Dashboard - PA crítica e médico vinculado")
@Composable
private fun DashboardPreviewCritico() {
    MotherCareTheme {
        DashboardScreenContent(
            DashboardUiState(
                carregando = false, dados = dadosExemplo.copy(medicoVinculado = true),
                statusPressao = UltimoStatusPressaoResponse("exemplo-id", "CRITICO", "2026-10-01T13:00:00Z")
            ),
            nome = "Maria Silva"
        )
    }
}

@Preview(showBackground = true, name = "Dashboard - erro")
@Composable
private fun DashboardPreviewErro() {
    MotherCareTheme { DashboardScreenContent(DashboardUiState(carregando = false, erro = "Gestante não encontrada"), onTentarNovamente = {}) }
}
