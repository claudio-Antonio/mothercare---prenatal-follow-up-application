package br.edu.ufmt.mothercare.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.edu.ufmt.mothercare.app.data.AppContainer
import br.edu.ufmt.mothercare.app.ui.auth.CadastroScreen
import br.edu.ufmt.mothercare.app.ui.auth.LoginScreen
import br.edu.ufmt.mothercare.app.ui.exame.ExameScreen
import br.edu.ufmt.mothercare.app.ui.gestante.CadastroGestanteScreen
import br.edu.ufmt.mothercare.app.ui.home.HomeScreen
import br.edu.ufmt.mothercare.app.ui.peso.PesoScreen
import br.edu.ufmt.mothercare.app.ui.prontuario.ProntuarioScreen

@Composable
fun MotherCareNavGraph(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = container.authRepository,
                aoLogar = { usuarioId -> navController.navigate(Routes.CADASTRO_GESTANTE + "?usuarioId=$usuarioId") },
                aoIrParaCadastro = { navController.navigate(Routes.CADASTRO) }
            )
        }

        composable(Routes.CADASTRO) {
            CadastroScreen(
                authRepository = container.authRepository,
                aoCadastrar = { usuarioId -> navController.navigate(Routes.CADASTRO_GESTANTE + "?usuarioId=$usuarioId") },
                aoVoltar = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CADASTRO_GESTANTE + "?usuarioId={usuarioId}",
            arguments = listOf(navArgument("usuarioId") { type = NavType.StringType })
        ) { backStackEntry ->
            val usuarioId = backStackEntry.arguments?.getString("usuarioId") ?: return@composable
            CadastroGestanteScreen(
                usuarioId = usuarioId,
                gestanteRepository = container.gestanteRepository,
                aoCadastrar = { gestanteId ->
                    navController.navigate(Routes.home(gestanteId)) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            )
        }

        composable(
            route = Routes.HOME,
            arguments = listOf(navArgument("gestanteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.getString("gestanteId") ?: return@composable
            HomeScreen(gestanteId = gestanteId, container = container, navController = navController)
        }

        composable(
            route = Routes.PESO,
            arguments = listOf(navArgument("gestanteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.getString("gestanteId") ?: return@composable
            TelaComVoltar(titulo = "Peso", aoVoltar = { navController.popBackStack() }) {
                PesoScreen(gestanteId, container.pesoRepository)
            }
        }

        composable(
            route = Routes.EXAME,
            arguments = listOf(navArgument("gestanteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.getString("gestanteId") ?: return@composable
            TelaComVoltar(titulo = "Exame laboratorial", aoVoltar = { navController.popBackStack() }) {
                ExameScreen(gestanteId, container.exameRepository)
            }
        }

        composable(
            route = Routes.PRONTUARIO,
            arguments = listOf(navArgument("gestanteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.getString("gestanteId") ?: return@composable
            TelaComVoltar(titulo = "Prontuário", aoVoltar = { navController.popBackStack() }) {
                ProntuarioScreen(gestanteId, container.prontuarioRepository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelaComVoltar(titulo: String, aoVoltar: () -> Unit, conteudo: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            conteudo()
        }
    }
}
