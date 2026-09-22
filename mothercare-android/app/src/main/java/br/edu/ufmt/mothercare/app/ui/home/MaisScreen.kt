package br.edu.ufmt.mothercare.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

private data class ItemMais(val titulo: String, val subtitulo: String, val icone: ImageVector, val onClick: () -> Unit)

@Composable
fun MaisScreen(
    aoAbrirPeso: () -> Unit,
    aoAbrirExame: () -> Unit,
    aoAbrirProntuario: () -> Unit
) {
    val itens = listOf(
        ItemMais("Monitoramento de peso", "Registrar peso e ver evolução (UC09)", Icons.Filled.MonitorWeight, aoAbrirPeso),
        ItemMais("Registrar exame laboratorial", "Hemoglobina e glicemia de jejum (UC08)", Icons.Filled.Science, aoAbrirExame),
        ItemMais("Prontuário digital", "Laudos e exames enviados (UC07)", Icons.Filled.Description, aoAbrirProntuario)
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Mais opções", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        itens.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { item.onClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(item.icone, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.titulo, fontWeight = FontWeight.SemiBold)
                        Text(item.subtitulo, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, name = "Mais")
@Composable
private fun MaisScreenPreview() {
    MotherCareTheme { MaisScreen({}, {}, {}) }
}
