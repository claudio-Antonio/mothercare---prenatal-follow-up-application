package br.edu.ufmt.mothercare.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme

data class Aba(val titulo: String, val icone: ImageVector)

val abasHome = listOf(
    Aba("Início", Icons.Filled.Home),
    Aba("Exames", Icons.Filled.CheckCircle),
    Aba("Check-in", Icons.Filled.MonitorHeart),
    Aba("Consultas", Icons.Filled.CalendarMonth),
    Aba("Mais", Icons.Filled.MoreHoriz)
)

@Composable
fun MotherCareBottomBar(abaSelecionada: Int, onAbaSelecionada: (Int) -> Unit) {
    NavigationBar {
        abasHome.forEachIndexed { indice, aba ->
            NavigationBarItem(
                selected = abaSelecionada == indice,
                onClick = { onAbaSelecionada(indice) },
                icon = { Icon(aba.icone, contentDescription = aba.titulo) },
                label = { Text(aba.titulo) }
            )
        }
    }
}

@Preview(showBackground = true, name = "Bottom bar - aba Inicio")
@Composable
private fun BottomBarPreview() {
    var selecionada by remember { mutableIntStateOf(0) }
    MotherCareTheme { MotherCareBottomBar(selecionada) { selecionada = it } }
}

@Preview(showBackground = true, name = "Bottom bar - aba Mais")
@Composable
private fun BottomBarPreviewMais() {
    MotherCareTheme { MotherCareBottomBar(abaSelecionada = 4, onAbaSelecionada = {}) }
}
