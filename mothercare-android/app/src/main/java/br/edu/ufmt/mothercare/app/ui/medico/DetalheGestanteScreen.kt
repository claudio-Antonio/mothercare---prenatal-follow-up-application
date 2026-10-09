package br.edu.ufmt.mothercare.app.ui.medico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.AppContainer
import br.edu.ufmt.mothercare.app.data.remote.dto.*
import br.edu.ufmt.mothercare.app.ui.agendamento.LinhaConsulta
import br.edu.ufmt.mothercare.app.ui.checkin.LinhaPressao
import br.edu.ufmt.mothercare.app.ui.checklist.StatusBadge
import br.edu.ufmt.mothercare.app.ui.components.*
import br.edu.ufmt.mothercare.app.ui.exame.LinhaExame
import br.edu.ufmt.mothercare.app.ui.peso.PesoLineChart
import br.edu.ufmt.mothercare.app.ui.prontuario.DocumentoItem
import br.edu.ufmt.mothercare.app.ui.prontuario.abrirArquivoExterno
import br.edu.ufmt.mothercare.app.ui.theme.*

private val ABAS = listOf("Resumo", "Exames", "Peso", "Laudos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheGestanteScreen(gestanteId: String, nome: String?, container: AppContainer, aoVoltar: () -> Unit) {
    val viewModel: DetalheGestanteViewModel = viewModel(
        factory = GenericViewModelFactory {
            DetalheGestanteViewModel(
                gestanteId, container.gestanteRepository, container.checkInRepository, container.agendamentoRepository,
                container.checklistRepository, container.exameRepository, container.pesoRepository, container.prontuarioRepository
            )
        }
    )
    val estado by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(estado.arquivoParaAbrir) {
        val abrir = estado.arquivoParaAbrir ?: return@LaunchedEffect
        viewModel.arquivoAberto()
        if (!abrirArquivoExterno(context, abrir)) viewModel.informarErroArquivo("Nenhum aplicativo instalado para abrir este arquivo.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nome ?: "Gestante") },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    IconButton(onClick = viewModel::carregar) { Icon(Icons.Filled.Refresh, contentDescription = "Atualizar") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            DetalheGestanteScreenContent(estado, onDocumentoClick = viewModel::abrirDocumento)
        }
    }
}

@Composable
fun DetalheGestanteScreenContent(
    estado: DetalheGestanteUiState,
    onDocumentoClick: (DocumentoClinicoResponse) -> Unit = {}
) {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = aba) {
            ABAS.forEachIndexed { indice, titulo ->
                Tab(selected = aba == indice, onClick = { aba = indice }, text = { Text(titulo) })
            }
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (aba) {
                0 -> AbaResumo(estado)
                1 -> AbaExames(estado)
                2 -> AbaPeso(estado.peso)
                3 -> AbaLaudos(estado, onDocumentoClick)
            }
        }
    }
}

@Composable
private fun AbaResumo(estado: DetalheGestanteUiState) {
    Secao(estado.idade) { dados ->
        Card(colors = CardDefaults.cardColors(containerColor = BlueSurfaceTint), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("${dados.semanas} semanas e ${dados.dias} dias · ${rotuloTrimestre(dados.trimestre)}",
                    fontWeight = FontWeight.Bold, color = BluePrimary)
                Text("DPP: ${formatarDataApi(dados.dataProvavelParto)}", style = MaterialTheme.typography.bodyMedium)
                Text("DUM: ${formatarDataApi(dados.dataUltimaMenstruacao)}", style = MaterialTheme.typography.bodyMedium)
                Text("IMC inicial: ${"%.1f".format(dados.imcInicial)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Titulo("Pressão arterial")
    Secao(estado.pressao, vazio = "Nenhum check-in de pressão registrado.") { registros ->
        registros.take(10).forEach { LinhaPressao(it) }
    }
    Titulo("Consultas")
    Secao(estado.consultas, vazio = "Nenhuma consulta agendada.") { consultas ->
        val validas = consultas.count { it.status != "CANCELADA" }
        Text("$validas de $MINIMO_CONSULTAS_PRE_NATAL consultas mínimas", style = MaterialTheme.typography.bodyMedium)
        consultas.forEach { LinhaConsulta(it) }
    }
}

@Composable
private fun AbaExames(estado: DetalheGestanteUiState) {
    Titulo("Checklist do trimestre", primeiro = true)
    Secao(estado.checklist) { checklist ->
        Text(rotuloTrimestre(checklist.trimestre), style = MaterialTheme.typography.bodyMedium)
        checklist.itens.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.nome, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                StatusBadge(item.status)
            }
        }
    }
    Titulo("Resultados laboratoriais")
    Secao(estado.exames, vazio = "Nenhum resultado registrado.") { exames -> exames.forEach { LinhaExame(it) } }
}

@Composable
private fun AbaPeso(peso: Carregavel<GraficoPesoResponse>) {
    Secao(peso) { grafico ->
        if (grafico.alertaGanhoExcessivo) {
            Card(colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))) {
                Text(grafico.mensagem ?: "Ganho de peso acima do esperado.", color = WarningAmber, modifier = Modifier.padding(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (grafico.pontos.isEmpty()) {
            Text("Nenhum peso registrado.", style = MaterialTheme.typography.bodyMedium)
        } else {
            if (grafico.pontos.size >= 2) {
                PesoLineChart(grafico.pontos, modifier = Modifier.fillMaxWidth().height(180.dp))
                Spacer(modifier = Modifier.height(12.dp))
            }
            grafico.pontos.reversed().forEach { ponto ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatarDataApi(ponto.data), style = MaterialTheme.typography.bodyMedium)
                    Text("${ponto.pesoKg} kg", fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AbaLaudos(estado: DetalheGestanteUiState, onDocumentoClick: (DocumentoClinicoResponse) -> Unit) {
    estado.erroArquivo?.let { ErrorBanner(it) }
    Secao(estado.documentos, vazio = "Nenhum laudo enviado pela gestante.") { documentos ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            documentos.forEach { doc ->
                DocumentoItem(doc, baixando = estado.baixandoId == doc.id, onClick = { onDocumentoClick(doc) })
            }
        }
    }
}

@Composable
private fun Titulo(texto: String, primeiro: Boolean = false) {
    if (!primeiro) Spacer(modifier = Modifier.height(20.dp))
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
}

/** Carregando / erro / vazio (listas) / conteúdo — o mesmo padrão em todas as seções. */
@Composable
private fun <T> Secao(estado: Carregavel<T>, vazio: String? = null, conteudo: @Composable (T) -> Unit) {
    val dados = estado.dados
    when {
        estado.carregando -> LoadingIndicator()
        estado.erro != null -> ErrorBanner(estado.erro)
        dados == null -> Unit
        vazio != null && dados is List<*> && dados.isEmpty() -> Text(vazio, style = MaterialTheme.typography.bodyMedium)
        else -> conteudo(dados)
    }
}

@Preview(showBackground = true, name = "Detalhe gestante - resumo")
@Composable
private fun DetalheGestantePreview() {
    MotherCareTheme {
        DetalheGestanteScreenContent(
            DetalheGestanteUiState(
                idade = Carregavel(false, IdadeGestacionalResponse("id", "2026-03-01", "2026-12-06", 31, 2, "TERCEIRO", 23.4)),
                pressao = Carregavel(false, listOf(RegistroPressaoResponse("p1", 150, 95, "CRITICO", "2026-10-05T12:00:00Z"))),
                consultas = Carregavel(false, listOf(ConsultaResponse("c1", "2026-10-20T09:00:00", "AGENDADA", 33))),
                checklist = Carregavel(false, ChecklistExamesResponse("id", "TERCEIRO", emptyList())),
                exames = Carregavel(false, emptyList()),
                peso = Carregavel(false, GraficoPesoResponse("id", emptyList(), false, null)),
                documentos = Carregavel(false, emptyList())
            )
        )
    }
}
