package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

@Serializable
data class RegistroPesoRequest(
    val gestanteId: String,
    val pesoKg: Double
)

@Serializable
data class PontoGraficoPeso(
    val data: String, // yyyy-MM-dd
    val pesoKg: Double
)

/** UC09 - RN05: alertaGanhoExcessivo = ganho > 1,5kg/mês no 2º trimestre. */
@Serializable
data class GraficoPesoResponse(
    val gestanteId: String,
    val pontos: List<PontoGraficoPeso>,
    val alertaGanhoExcessivo: Boolean,
    val mensagem: String?
)
