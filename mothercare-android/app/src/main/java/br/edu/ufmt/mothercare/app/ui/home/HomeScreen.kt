package br.edu.ufmt.mothercare.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.edu.ufmt.mothercare.app.data.AppContainer
import br.edu.ufmt.mothercare.app.ui.agendamento.AgendamentoScreen
import br.edu.ufmt.mothercare.app.ui.checkin.CheckInScreen
import br.edu.ufmt.mothercare.app.ui.checklist.ChecklistScreen
import br.edu.ufmt.mothercare.app.ui.dashboard.DashboardScreen

/**
 * Tela raiz pós-login: navegação inferior entre Dashboard (UC02),
 * Checklist de exames (UC06), Check-in de PA (UC04/RN02) e
 * Agendamento (UC03/RN04/RF04-RF05), todas escopadas na mesma
 * gestanteId. Sem @Preview aqui de propósito — depende do
 * AppContainer de verdade, e cada aba dispara chamada de rede real no
 * init{} do respectivo ViewModel. A barra inferior em si (parte
 * puramente visual) tem preview em BottomNavBar.kt.
 */
@Composable
fun HomeScreen(gestanteId: String, container: AppContainer) {
    var abaSelecionada by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = { MotherCareBottomBar(abaSelecionada) { abaSelecionada = it } }
    ) { paddingInterno ->
        Box(modifier = Modifier.padding(paddingInterno)) {
            when (abaSelecionada) {
                0 -> DashboardScreen(gestanteId, container.gestanteRepository)
                1 -> ChecklistScreen(gestanteId, container.checklistRepository)
                2 -> CheckInScreen(gestanteId, container.checkInRepository)
                3 -> AgendamentoScreen(gestanteId, container.agendamentoRepository)
            }
        }
    }
}
