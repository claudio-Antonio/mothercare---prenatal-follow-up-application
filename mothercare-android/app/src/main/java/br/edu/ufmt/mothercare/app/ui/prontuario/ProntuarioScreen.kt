package br.edu.ufmt.mothercare.app.ui.prontuario

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.DocumentoClinicoResponse
import br.edu.ufmt.mothercare.app.data.repository.ProntuarioRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen
import java.io.File

@Composable
fun ProntuarioScreen(gestanteId: String, prontuarioRepository: ProntuarioRepository) {
    val viewModel: ProntuarioViewModel = viewModel(
        factory = GenericViewModelFactory { ProntuarioViewModel(gestanteId, prontuarioRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var mostrarDialogoEnvio by remember { mutableStateOf(false) }
    var categoriaInput by remember { mutableStateOf("") }
    var uriSelecionada by remember { mutableStateOf<Uri?>(null) }

    val seletorArquivo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uriSelecionada = uri
        if (uri != null) mostrarDialogoEnvio = true
    }

    ProntuarioScreenContent(
        estado = estado,
        onNovoDocumentoClick = { seletorArquivo.launch("*/*") }
    )

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
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val extensao = if (mimeType.contains("pdf")) "pdf" else "jpg"
                    val arquivoTemp = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extensao")
                    context.contentResolver.openInputStream(uri)?.use { entrada ->
                        arquivoTemp.outputStream().use { saida -> entrada.copyTo(saida) }
                    }
                    viewModel.enviarDocumento(categoriaInput, arquivoTemp, mimeType)
                    mostrarDialogoEnvio = false
                    categoriaInput = ""
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
    onNovoDocumentoClick: () -> Unit
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
            Text("Laudos e exames enviados, do mais recente pro mais antigo.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            estado.mensagemSucesso?.let {
                Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))) {
                    Text(it, color = SuccessGreen, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            estado.erro?.let { ErrorBanner(it) }
            if (estado.enviando) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                estado.carregandoLista -> LoadingIndicator()
                estado.documentos.isEmpty() -> Text("Nenhum documento enviado ainda.", style = MaterialTheme.typography.bodyMedium)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estado.documentos) { doc -> DocumentoItem(doc) }
                }
            }
        }
    }
}

@Composable
private fun DocumentoItem(documento: DocumentoClinicoResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = BluePrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(documento.categoria, fontWeight = FontWeight.SemiBold)
                Text(documento.nomeArquivoOriginal, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${documento.tipoConteudo} · ${documento.tamanhoBytes / 1024} KB · ${documento.enviadoEm.take(10)}",
                    style = MaterialTheme.typography.labelLarge
                )
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
