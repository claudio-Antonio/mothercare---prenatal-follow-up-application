package br.edu.ufmt.mothercare.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import br.edu.ufmt.mothercare.app.data.AppContainer
import br.edu.ufmt.mothercare.app.ui.agendamento.AgendamentoScreen
import br.edu.ufmt.mothercare.app.ui.checkin.CheckInScreen
import br.edu.ufmt.mothercare.app.ui.checklist.ChecklistScreen
import br.edu.ufmt.mothercare.app.ui.dashboard.DashboardScreen
import br.edu.ufmt.mothercare.app.ui.navigation.Routes

private const val ABA_CHECK_IN = 2

@Composable
fun HomeScreen(gestanteId: String, container: AppContainer, navController: NavHostController, aoSair: () -> Unit) {
    var abaSelecionada by rememberSaveable { mutableIntStateOf(0) }
    val nome by container.sessionManager.nomeFlow.collectAsState(initial = null)

    Scaffold(
        bottomBar = { MotherCareBottomBar(abaSelecionada) { abaSelecionada = it } }
    ) { paddingInterno ->
        Box(modifier = Modifier.padding(paddingInterno)) {
            when (abaSelecionada) {
                0 -> DashboardScreen(
                    gestanteId, container.gestanteRepository, container.checkInRepository, nome,
                    aoIrParaCheckIn = { abaSelecionada = ABA_CHECK_IN }
                )
                1 -> ChecklistScreen(
                    gestanteId, container.checklistRepository,
                    aoEnviarLaudo = { nomeExame -> navController.navigate(Routes.prontuario(gestanteId, nomeExame)) }
                )
                2 -> CheckInScreen(gestanteId, container.checkInRepository)
                3 -> AgendamentoScreen(
                    gestanteId, container.agendamentoRepository, container.checkInRepository,
                    aoIrParaCheckIn = { abaSelecionada = ABA_CHECK_IN }
                )
                4 -> MaisScreen(
                    aoAbrirPeso = { navController.navigate(Routes.peso(gestanteId)) },
                    aoAbrirExame = { navController.navigate(Routes.exame(gestanteId)) },
                    aoAbrirProntuario = { navController.navigate(Routes.prontuario(gestanteId)) },
                    aoAbrirPerfil = { navController.navigate(Routes.PERFIL) },
                    aoSair = aoSair
                )
            }
        }
    }
}
