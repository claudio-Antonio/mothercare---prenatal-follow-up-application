package br.edu.ufmt.mothercare.app.ui.auth

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.repository.AuthRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

@Composable
fun CadastroScreen(
    authRepository: AuthRepository,
    aoCadastrar: (usuarioId: String) -> Unit,
    aoVoltar: () -> Unit
) {
    val viewModel: CadastroViewModel = viewModel(factory = GenericViewModelFactory { CadastroViewModel(authRepository) })
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado.cadastroConcluido) {
        if (estado.cadastroConcluido && estado.usuarioId != null) {
            aoCadastrar(estado.usuarioId!!)
        }
    }

    CadastroScreenContent(
        estado = estado,
        onNomeChange = viewModel::onNomeChange,
        onEmailChange = viewModel::onEmailChange,
        onSenhaChange = viewModel::onSenhaChange,
        onCadastrarClick = viewModel::cadastrar,
        onVoltarClick = aoVoltar
    )
}

@Composable
fun CadastroScreenContent(
    estado: CadastroUiState,
    onNomeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSenhaChange: (String) -> Unit,
    onCadastrarClick: () -> Unit,
    onVoltarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Criar conta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = estado.nome, onValueChange = onNomeChange,
            label = { Text("Nome completo") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = estado.email, onValueChange = onEmailChange,
            label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = estado.senha, onValueChange = onSenhaChange,
            label = { Text("Senha (mín. 8 caracteres)") }, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        estado.erro?.let { ErrorBanner(it) }

        PrimaryButton(texto = "Cadastrar", carregando = estado.carregando) { onCadastrarClick() }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onVoltarClick) { Text("Já tem conta? Entrar") }
    }
}

@Preview(showBackground = true, name = "Cadastro - vazio")
@Composable
private fun CadastroScreenPreviewVazio() {
    MotherCareTheme { CadastroScreenContent(CadastroUiState(), {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, name = "Cadastro - email ja existe")
@Composable
private fun CadastroScreenPreviewErro() {
    MotherCareTheme {
        CadastroScreenContent(
            CadastroUiState(nome = "Maria Teste", email = "maria@teste.com", senha = "senhaForte123",
                erro = "Já existe uma conta cadastrada com o email 'maria@teste.com'"),
            {}, {}, {}, {}, {}
        )
    }
}
