package br.edu.ufmt.mothercare.clinical.dto

import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate
import java.util.UUID

/** UC01: captura da DUM, peso inicial e altura. RF01. */
data class CadastroGestanteRequest(
    @field:NotNull
    val usuarioId: UUID,

    @field:NotNull
    @field:PastOrPresent(message = "A DUM não pode ser uma data futura")
    val dataUltimaMenstruacao: LocalDate,

    @field:DecimalMin(value = "20.0", message = "Peso inicial inválido")
    val pesoInicialKg: Double,

    @field:DecimalMin(value = "1.0", message = "Altura inválida")
    val alturaMetros: Double
)

/** UC02: retorno do cálculo de Idade Gestacional e DPP (RN01). */
data class IdadeGestacionalResponse(
    val gestanteId: UUID,
    val dataUltimaMenstruacao: LocalDate,
    val dataProvavelParto: LocalDate,
    val semanas: Long,
    val dias: Long,
    val trimestre: Trimestre,
    val imcInicial: Double
)
