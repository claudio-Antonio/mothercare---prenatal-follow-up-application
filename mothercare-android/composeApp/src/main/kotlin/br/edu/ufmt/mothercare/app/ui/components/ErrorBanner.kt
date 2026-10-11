package br.edu.ufmt.mothercare.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.edu.ufmt.mothercare.app.ui.theme.ErrorRed
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

/** [aoTentarNovamente] != null mostra o botão "Tentar novamente" (telas que carregam dados). */
@Composable
fun ErrorBanner(mensagem: String, aoTentarNovamente: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = mensagem,
                color = ErrorRed,
                style = MaterialTheme.typography.bodyMedium
            )
            if (aoTentarNovamente != null) {
                TextButton(onClick = aoTentarNovamente) { Text("Tentar novamente") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorBannerPreview() {
    MotherCareTheme { ErrorBanner("Credenciais inválidas. Verifique email e senha.") }
}

@Preview()
@Composable
private fun ErrorBannerRetryPreview() {
    MotherCareTheme { ErrorBanner("O servidor está indisponível no momento.", aoTentarNovamente = {}) }
}
