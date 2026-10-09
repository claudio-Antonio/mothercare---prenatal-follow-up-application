package br.edu.ufmt.mothercare.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import br.edu.ufmt.mothercare.app.data.AppContainer
import br.edu.ufmt.mothercare.app.data.remote.dto.Papel
import br.edu.ufmt.mothercare.app.ui.auth.CadastroScreen
import br.edu.ufmt.mothercare.app.ui.auth.DestinoLogin
import br.edu.ufmt.mothercare.app.ui.auth.LoginScreen
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.exame.ExameScreen
import br.edu.ufmt.mothercare.app.ui.gestante.CadastroGestanteScreen
import br.edu.ufmt.mothercare.app.ui.home.HomeScreen
import br.edu.ufmt.mothercare.app.ui.medico.DetalheGestanteScreen
import br.edu.ufmt.mothercare.app.ui.medico.MedicoDashboardScreen
import br.edu.ufmt.mothercare.app.ui.notificacoes.NotificacoesScreen
import br.edu.ufmt.mothercare.app.ui.perfil.PerfilScreen
import br.edu.ufmt.mothercare.app.ui.peso.PesoScreen
import br.edu.ufmt.mothercare.app.ui.prontuario.ProntuarioScreen
import kotlinx.coroutines.launch

@Composable
fun MotherCareNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val escopo = rememberCoroutineScope()

    val irParaLogin: (Boolean) -> Unit = { expirada ->
        navController.navigate(Routes.login(expirada)) { popUpTo(navController.graph.id) { inclusive = true } }
    }
    val sair: () -> Unit = {
        escopo.launch {
            container.authRepository.logout()
            irParaLogin(false)
        }
    }

    // Token expirado/inválido (401 numa rota autenticada): o ApiClient limpa a sessão e avisa aqui.
    LaunchedEffect(Unit) {
        container.sessionManager.sessaoExpirada.collect { irParaLogin(true) }
    }

    NavHost(navController = navController, startDestination = Routes.INICIO) {

        composable(Routes.INICIO) {
            LoadingIndicator()
            LaunchedEffect(Unit) {
                val sessao = container.sessionManager.obterSessao()
                val gestanteId = sessao.gestanteId
                val destino = when {
                    sessao.token.isNullOrBlank() -> Routes.login()
                    sessao.papel == Papel.MEDICO.name -> Routes.MEDICO_DASHBOARD
                    sessao.papel == Papel.GESTANTE.name && gestanteId != null -> Routes.home(gestanteId)
                    else -> Routes.login()
                }
                navController.navigate(destino) { popUpTo(Routes.INICIO) { inclusive = true } }
            }
        }

        composable(
            route = Routes.LOGIN,
            arguments = listOf(navArgument("expirada") { type = NavType.BoolType; defaultValue = false })
        ) { backStackEntry ->
            val expirada = backStackEntry.arguments?.read { getBoolean("expirada") } ?: false
            LoginScreen(
                authRepository = container.authRepository,
                gestanteRepository = container.gestanteRepository,
                mensagemInicial = if (expirada) "Sua sessão expirou. Entre novamente." else null,
                aoLogar = { destino ->
                    when (destino) {
                        DestinoLogin.PainelMedico -> navController.navigate(Routes.MEDICO_DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                        is DestinoLogin.HomeGestante -> navController.navigate(Routes.home(destino.gestanteId)) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                        is DestinoLogin.CadastroGestante ->
                            navController.navigate(Routes.CADASTRO_GESTANTE + "?usuarioId=${destino.usuarioId}")
                    }
                },
                aoIrParaCadastro = { navController.navigate(Routes.CADASTRO) }
            )
        }

        composable(Routes.CADASTRO) {
            // Cadastro público continua só pra gestante — contas de médico são
            // provisionadas por fora do app (exigem a chave de cadastro de médico).
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
            val usuarioId = backStackEntry.arguments?.read { getString("usuarioId") } ?: return@composable
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
            val gestanteId = backStackEntry.arguments?.read { getString("gestanteId") } ?: return@composable
            HomeScreen(gestanteId = gestanteId, container = container, navController = navController, aoSair = sair)
        }

        composable(
            route = Routes.PESO,
            arguments = listOf(navArgument("gestanteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.read { getString("gestanteId") } ?: return@composable
            TelaComVoltar(titulo = "Peso", aoVoltar = { navController.popBackStack() }) {
                PesoScreen(gestanteId, container.pesoRepository)
            }
        }

        composable(
            route = Routes.EXAME,
            arguments = listOf(navArgument("gestanteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.read { getString("gestanteId") } ?: return@composable
            TelaComVoltar(titulo = "Exame laboratorial", aoVoltar = { navController.popBackStack() }) {
                ExameScreen(gestanteId, container.exameRepository, container.prontuarioRepository)
            }
        }

        composable(
            route = Routes.PRONTUARIO,
            arguments = listOf(
                navArgument("gestanteId") { type = NavType.StringType },
                navArgument("categoria") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.read { getString("gestanteId") } ?: return@composable
            val categoria = backStackEntry.arguments?.read { getString("categoria") }
            TelaComVoltar(titulo = "Prontuário", aoVoltar = { navController.popBackStack() }) {
                ProntuarioScreen(gestanteId, container.prontuarioRepository, categoriaInicial = categoria)
            }
        }

        composable(Routes.PERFIL) {
            val nome by container.sessionManager.nomeFlow.collectAsState(initial = null)
            TelaComVoltar(titulo = "Meu perfil", aoVoltar = { navController.popBackStack() }) {
                PerfilScreen(container.gestanteRepository, nome)
            }
        }

        // RF08: notificações (gestante com lembrete de consulta; médico sem gestanteId)
        composable(
            route = Routes.NOTIFICACOES,
            arguments = listOf(navArgument("gestanteId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.read { getString("gestanteId") }
            TelaComVoltar(titulo = "Notificações", aoVoltar = { navController.popBackStack() }) {
                NotificacoesScreen(container.notificacaoRepository, container.agendamentoRepository, gestanteId)
            }
        }

        // Painel do médico
        composable(Routes.MEDICO_DASHBOARD) {
            val nome by container.sessionManager.nomeFlow.collectAsState(initial = null)
            MedicoDashboardScreen(
                medicoRepository = container.medicoRepository,
                aoSair = sair,
                nome = nome,
                aoAbrirGestante = { gestanteId, nomeGestante -> navController.navigate(Routes.medicoGestante(gestanteId, nomeGestante)) },
                aoAbrirNotificacoes = { navController.navigate(Routes.notificacoes(null)) }
            )
        }

        composable(
            route = Routes.MEDICO_GESTANTE,
            arguments = listOf(
                navArgument("gestanteId") { type = NavType.StringType },
                navArgument("nome") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val gestanteId = backStackEntry.arguments?.read { getString("gestanteId") } ?: return@composable
            DetalheGestanteScreen(
                gestanteId = gestanteId,
                nome = backStackEntry.arguments?.read { getString("nome") },
                container = container,
                aoVoltar = { navController.popBackStack() },
                aoRegistrarExame = { navController.navigate(Routes.exame(gestanteId)) },
                aoEnviarLaudo = { navController.navigate(Routes.prontuario(gestanteId)) }
            )
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
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            conteudo()
        }
    }
}
