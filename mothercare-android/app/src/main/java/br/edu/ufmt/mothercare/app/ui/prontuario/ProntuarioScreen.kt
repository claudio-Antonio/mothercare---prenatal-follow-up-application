package br.edu.ufmt.mothercare.app.ui.prontuario

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
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
import br.edu.ufmt.mothercare.app.data.remote.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.app.data.repository.ProntuarioRepository
import br.edu.ufmt.mothercare.app.data.repository.TIPOS_DOCUMENTO_ACEITOS
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.formatarDataApi
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen

@Composable
fun ProntuarioScreen(
    gestanteId: String,
    prontuarioRepository: ProntuarioRepository,
    /** Vindo do checklist: já preenche a categoria com o nome do exame e abre o seletor. */
    categoriaInicial: String? = null
) {
    val viewModel: ProntuarioViewModel = viewModel(
        factory = GenericViewModelFactory { ProntuarioViewModel(gestanteId, prontuarioRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var mostrarDialogoEnvio by remember { mutableStateOf(false) }
    var categoriaInput by remember { mutableStateOf(categoriaInicial.orEmpty()) }
    var uriSelecionada by remember { mutableStateOf<Uri?>(null) }
    var seletorAutomaticoAberto by rememberSaveable { mutableStateOf(false) }
    var documentoParaExcluir by remember { mutableStateOf<DocumentoClinicoResponse?>(null) }

    // OpenDocument filtra no próprio seletor os tipos aceitos pelo backend (PDF/JPEG/PNG).
    val seletorArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uriSelecionada = uri
        if (uri != null) mostrarDialogoEnvio = true
    }

    LaunchedEffect(categoriaInicial) {
        if (!categoriaInicial.isNullOrBlank() && !seletorAutomaticoAberto) {
            seletorAutomaticoAberto = true
            seletorArquivo.launch(TIPOS_DOCUMENTO_ACEITOS)
        }
    }

    LaunchedEffect(estado.arquivoParaAbrir) {
        val abrir = estado.arquivoParaAbrir ?: return@LaunchedEffect
        viewModel.arquivoAberto()
        if (!abrirArquivoExterno(context, abrir)) viewModel.informarErro("Nenhum aplicativo instalado para abrir este arquivo.")
    }

    ProntuarioScreenContent(
        estado = estado,
        onNovoDocumentoClick = { seletorArquivo.launch(TIPOS_DOCUMENTO_ACEITOS) },
        onDocumentoClick = viewModel::abrirDocumento,
        onExcluirClick = { documentoParaExcluir = it },
        onTentarNovamente = viewModel::carregarLista
    )

    documentoParaExcluir?.let { documento ->
        AlertDialog(
            onDismissRequest = { documentoParaExcluir = null },
            title = { Text("Excluir documento?") },
            text = { Text("\"${documento.nomeArquivoOriginal}\" será removido do prontuário. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { viewModel.excluir(documento); documentoParaExcluir = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { documentoParaExcluir = null }) { Text("Cancelar") } }
        )
    }

    if (mostrarDialogoEnvio && uriSelecionada != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEnvio = false },
            title = { Text("Categoria do documento") },
            text = {
                OutlinedTextField(
                    value = categoriaInput,
                    onValueChange = { categoriaInput = it },
                    label = { Text("Ex: Hemograma completo") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = uriSelecionada ?: return@TextButton
                    viewModel.enviarDocumento(categoriaInput, uri)
                    mostrarDialogoEnvio = false
                    categoriaInput = categoriaInicial.orEmpty()
                }) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEnvio = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ProntuarioScreenContent(
    estado: ProntuarioUiState,
    onNovoDocumentoClick: () -> Unit,
    onDocumentoClick: (DocumentoClinicoResponse) -> Unit = {},
    onExcluirClick: ((DocumentoClinicoResponse) -> Unit)? = null,
    onTentarNovamente: (() -> Unit)? = null
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNovoDocumentoClick, containerColor = BluePrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar documento")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Prontuário digital", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Laudos e exames enviados, do mais recente pro mais antigo. Toque para abrir.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            estado.mensagemSucesso?.let {
                Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))) {
                    Text(it, color = SuccessGreen, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            estado.erro?.let { ErrorBanner(it, onTentarNovamente.takeIf { estado.documentos.isEmpty() }) }
            if (estado.enviando) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                estado.carregandoLista -> LoadingIndicator()
                estado.documentos.isEmpty() -> Text("Nenhum documento enviado ainda.", style = MaterialTheme.typography.bodyMedium)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.documentos) { doc ->
                        DocumentoItem(
                            doc, baixando = estado.baixandoId == doc.id, onClick = { onDocumentoClick(doc) },
                            onExcluir = onExcluirClick?.let { excluir -> { excluir(doc) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DocumentoItem(
    documento: DocumentoClinicoResponse,
    baixando: Boolean,
    onClick: () -> Unit,
    onExcluir: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(enabled = !baixando, onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = BluePrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(documento.categoria, fontWeight = FontWeight.SemiBold)
                Text(documento.nomeArquivoOriginal, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${documento.tipoConteudo} · ${documento.tamanhoBytes / 1024} KB · ${formatarDataApi(documento.enviadoEm)}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (baixando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (onExcluir != null) {
                IconButton(onClick = onExcluir) { Icon(Icons.Filled.Delete, contentDescription = "Excluir documento") }
            }
        }
    }
}

private val documentosExemplo = listOf(
    DocumentoClinicoResponse("1", "exemplo", "Hemograma completo", "hemograma.pdf", "PDF", 245_000, "2026-08-01T10:00:00"),
    DocumentoClinicoResponse("2", "exemplo", "Ultrassonografia obstétrica inicial", "usg.jpg", "IMAGEM", 1_200_000, "2026-07-15T14:30:00")
)

@Preview(showBackground = true, name = "Prontuario - vazio")
@Composable
private fun ProntuarioPreviewVazio() {
    MotherCareTheme { ProntuarioScreenContent(ProntuarioUiState(carregandoLista = false, documentos = emptyList()), {}) }
}

@Preview(showBackground = true, name = "Prontuario - com documentos")
@Composable
private fun ProntuarioPreviewComDados() {
    MotherCareTheme { ProntuarioScreenContent(ProntuarioUiState(carregandoLista = false, documentos = documentosExemplo), {}) }
}
