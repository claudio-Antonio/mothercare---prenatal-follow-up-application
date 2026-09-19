package br.edu.ufmt.mothercare.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.edu.ufmt.mothercare.app.ui.theme.ErrorRed
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

@Composable
fun ErrorBanner(mensagem: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
    ) {
        Text(
            text = mensagem,
            color = ErrorRed,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorBannerPreview() {
    MotherCareTheme { ErrorBanner("Credenciais inválidas. Verifique email e senha.") }
}
