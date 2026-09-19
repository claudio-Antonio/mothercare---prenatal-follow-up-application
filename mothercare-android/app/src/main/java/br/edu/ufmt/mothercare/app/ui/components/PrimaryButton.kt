package br.edu.ufmt.mothercare.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

@Composable
fun PrimaryButton(
    texto: String,
    carregando: Boolean = false,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !carregando,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        if (carregando) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    MotherCareTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(texto = "Entrar", carregando = false) {}
            PrimaryButton(texto = "Carregando...", carregando = true) {}
        }
    }
}
