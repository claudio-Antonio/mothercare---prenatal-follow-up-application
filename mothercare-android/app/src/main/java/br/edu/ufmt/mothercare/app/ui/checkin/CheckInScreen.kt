package br.edu.ufmt.mothercare.app.ui.checkin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.ResultadoTriagemPressaoResponse
import br.edu.ufmt.mothercare.app.data.repository.CheckInRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.ErrorRed
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen

@Composable
fun CheckInScreen(gestanteId: String, checkInRepository: CheckInRepository) {
    val viewModel: CheckInViewModel = viewModel(
        factory = GenericViewModelFactory { CheckInViewModel(gestanteId, checkInRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    CheckInScreenContent(
        estado = estado,
        onSistolicaChange = viewModel::onSistolicaChange,
        onDiastolicaChange = viewModel::onDiastolicaChange,
        onRegistrarClick = viewModel::registrar
    )
}

@Composable
fun CheckInScreenContent(
    estado: CheckInUiState,
    onSistolicaChange: (String) -> Unit,
    onDiastolicaChange: (String) -> Unit,
    onRegistrarClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Check-in de pressão arterial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = estado.sistolica, onValueChange = onSistolicaChange,
            label = { Text("Pressão sistólica (mmHg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = estado.diastolica, onValueChange = onDiastolicaChange,
            label = { Text("Pressão diastólica (mmHg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        estado.erro?.let { ErrorBanner(it) }

        PrimaryButton(texto = "Registrar", carregando = estado.carregando) { onRegistrarClick() }

        estado.resultado?.let { resultado ->
            Spacer(modifier = Modifier.height(20.dp))
            val critico = resultado.nivelRisco == "CRITICO"
            Card(colors = CardDefaults.cardColors(containerColor = if (critico) ErrorRed.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (critico) "⚠ Risco detectado" else "✓ Pressão normal",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (critico) ErrorRed else SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(resultado.mensagem, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "CheckIn - vazio")
@Composable
private fun CheckInPreviewVazio() {
    MotherCareTheme { CheckInScreenContent(CheckInUiState(), {}, {}, {}) }
}

@Preview(showBackground = true, name = "CheckIn - resultado normal")
@Composable
private fun CheckInPreviewNormal() {
    MotherCareTheme {
        CheckInScreenContent(
            CheckInUiState(
                sistolica = "120", diastolica = "80",
                resultado = ResultadoTriagemPressaoResponse("NORMAL", true, "Pressão arterial dentro dos parâmetros esperados.")
            ),
            {}, {}, {}
        )
    }
}

@Preview(showBackground = true, name = "CheckIn - resultado critico")
@Composable
private fun CheckInPreviewCritico() {
    MotherCareTheme {
        CheckInScreenContent(
            CheckInUiState(
                sistolica = "150", diastolica = "95",
                resultado = ResultadoTriagemPressaoResponse(
                    "CRITICO", false,
                    "Risco iminente de Síndrome Hipertensiva/Pré-eclâmpsia detectado (PA 150/95 mmHg). Agendamento eletivo bloqueado — procure atendimento hospitalar imediato."
                )
            ),
            {}, {}, {}
        )
    }
}
