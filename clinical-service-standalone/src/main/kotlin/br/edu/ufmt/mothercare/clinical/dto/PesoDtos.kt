package br.edu.ufmt.mothercare.clinical.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

/** UC09: novo registro de peso da gestante. */
data class RegistroPesoRequest(
    @field:NotNull
    val gestanteId: UUID,

    @field:DecimalMin(value = "20.0")
    val pesoKg: Double
)

data class PontoGraficoPeso(
    val data: LocalDate,
    val pesoKg: Double
)

/**
 * RN05: alerta nutricional caso o ganho ultrapasse 1,5 kg/mês no
 * 2º trimestre.
 */
data class GraficoPesoResponse(
    val gestanteId: UUID,
    val pontos: List<PontoGraficoPeso>,
    val alertaGanhoExcessivo: Boolean,
    val mensagem: String?
)
