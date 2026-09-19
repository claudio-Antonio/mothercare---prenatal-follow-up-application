package br.edu.ufmt.mothercare.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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

/** Wrapper conectado ao repositório/ViewModel — usado de verdade na navegação. */
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    aoLogar: (usuarioId: String) -> Unit,
    aoIrParaCadastro: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel(factory = GenericViewModelFactory { LoginViewModel(authRepository) })
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado.loginConcluido) {
        if (estado.loginConcluido && estado.usuarioId != null) {
            aoLogar(estado.usuarioId!!)
        }
    }

    LoginScreenContent(
        estado = estado,
        onEmailChange = viewModel::onEmailChange,
        onSenhaChange = viewModel::onSenhaChange,
        onLoginClick = viewModel::login,
        onCadastroClick = aoIrParaCadastro
    )
}

/** Parte "burra": só desenha a partir do estado — é o que o @Preview usa. */
@Composable
fun LoginScreenContent(
    estado: LoginUiState,
    onEmailChange: (String) -> Unit,
    onSenhaChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onCadastroClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Mother Care", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
        Text("Acompanhamento pré-natal inteligente", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = estado.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = estado.senha,
            onValueChange = onSenhaChange,
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        estado.erro?.let { ErrorBanner(it) }

        PrimaryButton(texto = "Entrar", carregando = estado.carregando) { onLoginClick() }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCadastroClick) { Text("Ainda não tem conta? Cadastre-se") }
    }
}

@Preview(showBackground = true, name = "Login - vazio")
@Composable
private fun LoginScreenPreviewVazio() {
    MotherCareTheme {
        LoginScreenContent(LoginUiState(), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Login - preenchido")
@Composable
private fun LoginScreenPreviewPreenchido() {
    MotherCareTheme {
        LoginScreenContent(LoginUiState(email = "maria@teste.com", senha = "senhaForte123"), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Login - carregando")
@Composable
private fun LoginScreenPreviewCarregando() {
    MotherCareTheme {
        LoginScreenContent(LoginUiState(email = "maria@teste.com", senha = "senhaForte123", carregando = true), {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Login - erro")
@Composable
private fun LoginScreenPreviewErro() {
    MotherCareTheme {
        LoginScreenContent(
            LoginUiState(email = "maria@teste.com", senha = "errada", erro = "Email ou senha inválidos"),
            {}, {}, {}, {}
        )
    }
}
