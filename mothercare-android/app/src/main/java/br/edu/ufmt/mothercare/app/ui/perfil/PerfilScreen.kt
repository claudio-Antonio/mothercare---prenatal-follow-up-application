package br.edu.ufmt.mothercare.app.ui.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.IdadeGestacionalResponse
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import br.edu.ufmt.mothercare.app.ui.components.CampoData
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen
import java.time.LocalDate

@Composable
fun PerfilScreen(gestanteRepository: GestanteRepository, nome: String?) {
    val viewModel: PerfilViewModel = viewModel(factory = GenericViewModelFactory { PerfilViewModel(gestanteRepository) })
    val estado by viewModel.uiState.collectAsState()
    PerfilScreenContent(
        estado = estado,
        nome = nome,
        onDumChange = viewModel::onDumChange,
        onPesoChange = viewModel::onPesoChange,
        onAlturaChange = viewModel::onAlturaChange,
        onSalvarClick = viewModel::salvar,
        onDesvincularClick = viewModel::desvincularMedico,
        onTentarNovamente = viewModel::carregar
    )
}

@Composable
fun PerfilScreenContent(
    estado: PerfilUiState,
    nome: String?,
    onDumChange: (String) -> Unit,
    onPesoChange: (String) -> Unit,
    onAlturaChange: (String) -> Unit,
    onSalvarClick: () -> Unit,
    onDesvincularClick: () -> Unit,
    onTentarNovamente: (() -> Unit)? = null
) {
    var confirmarDesvinculo by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        nome?.let { Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary) }
        Spacer(modifier = Modifier.height(16.dp))

        when {
            estado.carregando -> LoadingIndicator()
            estado.dados == null -> ErrorBanner(estado.erro ?: "Não foi possível carregar seus dados.", onTentarNovamente)
            else -> {
                val dados = estado.dados
                Text("Dados do pré-natal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Corrigir a DUM recalcula a idade gestacional, o trimestre, a data provável do parto e o checklist de exames.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                CampoData(
                    valorIso = estado.dum,
                    rotulo = "Data da última menstruação",
                    onDataSelecionada = onDumChange,
                    permitir = { data -> !data.isAfter(LocalDate.now()) && data.isAfter(LocalDate.now().minusWeeks(44)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = estado.peso, onValueChange = onPesoChange, label = { Text("Peso inicial (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = estado.altura, onValueChange = onAlturaChange, label = { Text("Altura (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                estado.erro?.let { ErrorBanner(it) }
                estado.mensagemSucesso?.let { Text(it, color = SuccessGreen, style = MaterialTheme.typography.bodyMedium) }
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(texto = "Salvar alterações", carregando = estado.salvando) { onSalvarClick() }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Médico responsável", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                if (dados.medicoVinculado) {
                    Text("Um médico acompanha seus dados pelo painel.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { confirmarDesvinculo = true }, enabled = !estado.salvando) { Text("Desvincular médico") }
                } else {
                    Text(
                        "Nenhum médico vinculado. Envie o código abaixo ao seu médico para ele acompanhar seus dados.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dados.gestanteId, style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { clipboard.setText(AnnotatedString(dados.gestanteId)) }) { Text("Copiar código") }
                }
            }
        }
    }

    if (confirmarDesvinculo) {
        AlertDialog(
            onDismissRequest = { confirmarDesvinculo = false },
            title = { Text("Desvincular médico?") },
            text = { Text("Ele deixará de ver seus dados no painel. Você poderá vincular outro médico com o seu código.") },
            confirmButton = { TextButton(onClick = { onDesvincularClick(); confirmarDesvinculo = false }) { Text("Desvincular") } },
            dismissButton = { TextButton(onClick = { confirmarDesvinculo = false }) { Text("Cancelar") } }
        )
    }
}

private val dadosExemplo = IdadeGestacionalResponse(
    gestanteId = "a1b2c3d4-1111-4e5f-8a9b-000000000001", dataUltimaMenstruacao = "2026-05-01",
    dataProvavelParto = "2027-02-05", semanas = 22, dias = 4, trimestre = "SEGUNDO", imcInicial = 22.0,
    pesoInicialKg = 60.0, alturaMetros = 1.65
)

@Preview(showBackground = true, name = "Perfil - sem médico")
@Composable
private fun PerfilPreviewSemMedico() {
    MotherCareTheme {
        PerfilScreenContent(
            PerfilUiState(carregando = false, dados = dadosExemplo, dum = "2026-05-01", peso = "60.0", altura = "1.65"),
            "Maria Silva", {}, {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true, name = "Perfil - com médico")
@Composable
private fun PerfilPreviewComMedico() {
    MotherCareTheme {
        PerfilScreenContent(
            PerfilUiState(carregando = false, dados = dadosExemplo.copy(medicoVinculado = true), dum = "2026-05-01", peso = "60.0", altura = "1.65"),
            "Maria Silva", {}, {}, {}, {}, {}
        )
    }
}
