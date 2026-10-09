package br.edu.ufmt.mothercare.app.ui.exame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameLaboratorialResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.TipoExame
import br.edu.ufmt.mothercare.app.data.remote.dto.ExameHistoricoResponse
import br.edu.ufmt.mothercare.app.data.repository.ExameRepository
import br.edu.ufmt.mothercare.app.data.repository.ProntuarioRepository
import br.edu.ufmt.mothercare.app.ui.prontuario.EXTENSOES_DOCUMENTO_ACEITAS
import br.edu.ufmt.mothercare.app.ui.prontuario.paraArquivoEscolhido
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import br.edu.ufmt.mothercare.app.ui.components.formatarDataHoraApi
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.ErrorRed
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.SuccessGreen

@Composable
fun ExameScreen(gestanteId: String, exameRepository: ExameRepository, prontuarioRepository: ProntuarioRepository) {
    val viewModel: ExameViewModel = viewModel(
        factory = GenericViewModelFactory { ExameViewModel(gestanteId, exameRepository, prontuarioRepository) }
    )
    val estado by viewModel.uiState.collectAsState()
    val escopo = rememberCoroutineScope()
    val seletorLaudo = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = EXTENSOES_DOCUMENTO_ACEITAS)
    ) { arquivo ->
        if (arquivo != null) escopo.launch { viewModel.onLaudoSelecionado(arquivo.paraArquivoEscolhido()) }
    }
    ExameScreenContent(
        estado = estado,
        onTipoChange = viewModel::onTipoChange,
        onValorChange = viewModel::onValorChange,
        onRegistrarClick = viewModel::registrar,
        onAnexarLaudo = { seletorLaudo.launch() },
        onRemoverLaudo = { viewModel.onLaudoSelecionado(null) }
    )
}

@Composable
fun ExameScreenContent(
    estado: ExameUiState,
    onTipoChange: (TipoExame) -> Unit,
    onValorChange: (String) -> Unit,
    onRegistrarClick: () -> Unit,
    onAnexarLaudo: () -> Unit = {},
    onRemoverLaudo: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Registrar exame laboratorial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Tipo de exame", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Column {
            listOf(
                TipoExame.HEMOGLOBINA to "Hemoglobina (g/dL)",
                TipoExame.GLICEMIA_JEJUM to "Glicemia de jejum (mg/dL)"
            ).forEach { (tipo, rotulo) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = estado.tipo == tipo, onClick = { onTipoChange(tipo) })
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(selected = estado.tipo == tipo, onClick = { onTipoChange(tipo) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rotulo, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = estado.valorInput,
            onValueChange = onValorChange,
            label = { Text("Valor medido") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (estado.laudoSelecionado == null) {
            OutlinedButton(onClick = onAnexarLaudo, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Anexar laudo (opcional)")
            }
        } else {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, tint = BluePrimary)
                Text(
                    "Laudo anexado — vai para o prontuário como \"${categoriaDoLaudo(estado.tipo)}\"",
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRemoverLaudo) { Text("Remover") }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        estado.erro?.let { ErrorBanner(it) }

        PrimaryButton(texto = "Registrar", carregando = estado.carregando) { onRegistrarClick() }

        estado.resultado?.let { resultado ->
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (resultado.alertaDisparado) ErrorRed.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (resultado.alertaDisparado) "⚠ Alerta disparado" else "✓ Dentro da normalidade",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (resultado.alertaDisparado) ErrorRed else SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(resultado.mensagem, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (estado.historico.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Resultados anteriores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            estado.historico.forEach { LinhaExame(it) }
        }
    }
}

/** Também usada no detalhe da gestante do painel do médico. */
@Composable
internal fun LinhaExame(exame: ExameHistoricoResponse) {
    val unidade = if (exame.tipo == TipoExame.HEMOGLOBINA) "g/dL" else "mg/dL"
    val nome = if (exame.tipo == TipoExame.HEMOGLOBINA) "Hemoglobina" else "Glicemia de jejum"
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(nome, fontWeight = FontWeight.SemiBold)
            Text(
                formatarDataHoraApi(exame.registradoEm) + if (exame.documentoId != null) " · com laudo" else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            "${exame.valor} $unidade" + if (exame.alertaDisparado) " ⚠" else "",
            fontWeight = FontWeight.SemiBold,
            color = if (exame.alertaDisparado) ErrorRed else SuccessGreen
        )
    }
    HorizontalDivider()
}

@Preview()
@Composable
private fun ExamePreviewVazio() {
    MotherCareTheme { ExameScreenContent(ExameUiState(), {}, {}, {}) }
}

@Preview()
@Composable
private fun ExamePreviewAlertaAnemia() {
    MotherCareTheme {
        ExameScreenContent(
            ExameUiState(
                tipo = TipoExame.HEMOGLOBINA, valorInput = "9.5",
                resultado = ExameLaboratorialResponse(
                    id = "exemplo", tipo = TipoExame.HEMOGLOBINA, valor = 9.5, alertaDisparado = true,
                    mensagem = "Alerta de possível anemia: Hb = 9.5 g/dL (limiar < 11.0 g/dL)."
                )
            ),
            {}, {}, {}
        )
    }
}

@Preview()
@Composable
private fun ExamePreviewNormal() {
    MotherCareTheme {
        ExameScreenContent(
            ExameUiState(
                tipo = TipoExame.GLICEMIA_JEJUM, valorInput = "85",
                resultado = ExameLaboratorialResponse(
                    id = "exemplo", tipo = TipoExame.GLICEMIA_JEJUM, valor = 85.0, alertaDisparado = false,
                    mensagem = "Valor dentro da normalidade."
                )
            ),
            {}, {}, {}
        )
    }
}
