package br.edu.ufmt.mothercare.app.ui.peso

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ufmt.mothercare.app.data.remote.dto.GraficoPesoResponse
import br.edu.ufmt.mothercare.app.data.remote.dto.PontoGraficoPeso
import br.edu.ufmt.mothercare.app.data.repository.PesoRepository
import br.edu.ufmt.mothercare.app.ui.components.ErrorBanner
import br.edu.ufmt.mothercare.app.ui.components.GenericViewModelFactory
import br.edu.ufmt.mothercare.app.ui.components.LoadingIndicator
import br.edu.ufmt.mothercare.app.ui.components.PrimaryButton
import br.edu.ufmt.mothercare.app.ui.theme.BluePrimary
import br.edu.ufmt.mothercare.app.ui.theme.MotherCareTheme
import br.edu.ufmt.mothercare.app.ui.theme.WarningAmber

@Composable
fun PesoScreen(gestanteId: String, pesoRepository: PesoRepository) {
    val viewModel: PesoViewModel = viewModel(factory = GenericViewModelFactory { PesoViewModel(gestanteId, pesoRepository) })
    val estado by viewModel.uiState.collectAsState()
    PesoScreenContent(
        estado = estado,
        onPesoInputChange = viewModel::onPesoInputChange,
        onRegistrarClick = viewModel::registrarPeso
    )
}

@Composable
fun PesoScreenContent(
    estado: PesoUiState,
    onPesoInputChange: (String) -> Unit,
    onRegistrarClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Monitoramento de peso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BluePrimary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = estado.pesoInput,
            onValueChange = onPesoInputChange,
            label = { Text("Peso atual (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        estado.erro?.let { ErrorBanner(it) }

        PrimaryButton(texto = "Registrar peso", carregando = estado.registrando) { onRegistrarClick() }
        Spacer(modifier = Modifier.height(24.dp))

        when {
            estado.carregandoGrafico -> LoadingIndicator()
            estado.grafico != null -> {
                val grafico = estado.grafico

                if (grafico.alertaGanhoExcessivo) {
                    Card(colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚠ Ganho de peso acima do esperado", fontWeight = FontWeight.Bold, color = WarningAmber)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(grafico.mensagem ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text("Evolução do peso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                if (grafico.pontos.size >= 2) {
                    PesoLineChart(pontos = grafico.pontos, modifier = Modifier.fillMaxWidth().height(180.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                grafico.pontos.reversed().forEach { ponto ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ponto.data, style = MaterialTheme.typography.bodyMedium)
                        Text("${ponto.pesoKg} kg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PesoLineChart(pontos: List<PontoGraficoPeso>, modifier: Modifier = Modifier) {
    val corLinha = BluePrimary
    Canvas(modifier = modifier) {
        val pesos = pontos.map { it.pesoKg }
        val min = (pesos.minOrNull() ?: 0.0).minus(1.0).toFloat()
        val max = (pesos.maxOrNull() ?: 0.0).plus(1.0).toFloat()
        val faixa = (max - min).takeIf { it > 0f } ?: 1f

        val larguraPasso = size.width / (pontos.size - 1).coerceAtLeast(1)

        val coordenadas = pontos.mapIndexed { indice, ponto ->
            val x = indice * larguraPasso
            val yNormalizado = (ponto.pesoKg.toFloat() - min) / faixa
            val y = size.height - (yNormalizado * size.height)
            Offset(x, y)
        }

        for (i in 0 until coordenadas.size - 1) {
            drawLine(color = corLinha, start = coordenadas[i], end = coordenadas[i + 1], strokeWidth = 5f, cap = StrokeCap.Round)
        }
        coordenadas.forEach { ponto -> drawCircle(color = corLinha, radius = 8f, center = ponto) }
    }
}

private val graficoExemplo = GraficoPesoResponse(
    gestanteId = "exemplo-id",
    pontos = listOf(
        PontoGraficoPeso("2026-06-01", 60.0),
        PontoGraficoPeso("2026-07-01", 62.0),
        PontoGraficoPeso("2026-08-01", 65.5)
    ),
    alertaGanhoExcessivo = true,
    mensagem = "Ganho de peso acima de 1,5 kg/mês detectado no 2º trimestre. Recomenda-se orientação nutricional."
)

@Preview(showBackground = true, name = "Peso - carregando")
@Composable
private fun PesoPreviewCarregando() {
    MotherCareTheme { PesoScreenContent(PesoUiState(carregandoGrafico = true), {}, {}) }
}

@Preview(showBackground = true, name = "Peso - com alerta")
@Composable
private fun PesoPreviewComAlerta() {
    MotherCareTheme { PesoScreenContent(PesoUiState(carregandoGrafico = false, grafico = graficoExemplo), {}, {}) }
}

@Preview(showBackground = true, name = "Peso - sem registros ainda")
@Composable
private fun PesoPreviewVazio() {
    MotherCareTheme {
        PesoScreenContent(
            PesoUiState(carregandoGrafico = false, grafico = graficoExemplo.copy(pontos = emptyList(), alertaGanhoExcessivo = false, mensagem = null)),
            {}, {}
        )
    }
}
