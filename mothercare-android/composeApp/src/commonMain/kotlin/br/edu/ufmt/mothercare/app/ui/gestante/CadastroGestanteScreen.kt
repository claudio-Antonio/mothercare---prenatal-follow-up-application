package br.edu.ufmt.mothercare.app.ui.gestante

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.repository.GestanteRepository
import br.edu.ufmt.mothercare.app.ui.components.CampoData
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.components.hojeNoAparelho
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@Composable
fun CadastroGestanteScreen(
    usuarioId: String,
    gestanteRepository: GestanteRepository,
    aoCadastrar: (gestanteId: String) -> Unit
) {
    val viewModel: CadastroGestanteViewModel = viewModel(
        factory = GenericViewModelFactory { CadastroGestanteViewModel(usuarioId, gestanteRepository) }
    )
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado.gestanteIdCriada) {
        estado.gestanteIdCriada?.let {
            viewModel.navegacaoConsumida()
            aoCadastrar(it)
        }
    }

    CadastroGestanteScreenContent(
        estado = estado,
        onDumChange = viewModel::onDumChange,
        onPesoChange = viewModel::onPesoChange,
        onAlturaChange = viewModel::onAlturaChange,
        onContinuarClick = viewModel::cadastrar
    )
}

@Composable
fun CadastroGestanteScreenContent(
    estado: CadastroGestanteUiState,
    onDumChange: (String) -> Unit,
    onPesoChange: (String) -> Unit,
    onAlturaChange: (String) -> Unit,
    onContinuarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vamos começar seu acompanhamento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Precisamos de alguns dados iniciais pra calcular sua idade gestacional.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))

        // DUM: não pode ser futura (@PastOrPresent no backend) nem anterior a ~44 semanas.
        CampoData(
            valorIso = estado.dum,
            rotulo = "Data da última menstruação",
            onDataSelecionada = onDumChange,
            permitir = { data -> data <= hojeNoAparelho() && data > hojeNoAparelho().minus(44, DateTimeUnit.WEEK) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = estado.peso, onValueChange = onPesoChange,
            label = { Text("Peso inicial (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = estado.altura, onValueChange = onAlturaChange,
            label = { Text("Altura (m)") }, placeholder = { Text("ex: 1.65") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        estado.erro?.let { ErrorBanner(it) }

        PrimaryButton(texto = "Continuar", carregando = estado.carregando) { onContinuarClick() }
    }
}

@Preview()
@Composable
private fun CadastroGestantePreviewVazio() {
    MotherCareTheme { CadastroGestanteScreenContent(CadastroGestanteUiState(), {}, {}, {}, {}) }
}

@Preview()
@Composable
private fun CadastroGestantePreviewPreenchido() {
    MotherCareTheme {
        CadastroGestanteScreenContent(
            CadastroGestanteUiState(dum = "2025-11-15", peso = "60", altura = "1.65"),
            {}, {}, {}, {}
        )
    }
}
