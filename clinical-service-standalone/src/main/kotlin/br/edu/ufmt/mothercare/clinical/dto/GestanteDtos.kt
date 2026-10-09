package br.edu.ufmt.mothercare.clinical.dto

import br.edu.ufmt.mothercare.clinical.entity.NivelRisco
import br.edu.ufmt.mothercare.clinical.entity.Trimestre
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate
import java.util.UUID

data class CadastroGestanteRequest(
    @field:NotNull
    val usuarioId: UUID,

    @field:NotNull
    @field:PastOrPresent(message = "A DUM não pode ser uma data futura")
    val dataUltimaMenstruacao: LocalDate,

    @field:DecimalMin(value = "20.0", message = "Peso inicial inválido")
    val pesoInicialKg: Double,

    @field:DecimalMin(value = "1.0", message = "Altura inválida")
    val alturaMetros: Double,

    /** Opcional — vincula a gestante a um médico já no cadastro. */
    val medicoId: UUID? = null
)

/** Perfil: correção dos dados clínicos iniciais pela própria gestante (PUT /gestantes/me). */
data class AtualizarGestanteRequest(
    @field:NotNull
    @field:PastOrPresent(message = "A DUM não pode ser uma data futura")
    val dataUltimaMenstruacao: LocalDate,

    @field:DecimalMin(value = "20.0", message = "Peso inicial inválido")
    val pesoInicialKg: Double,

    @field:DecimalMin(value = "1.0", message = "Altura inválida")
    val alturaMetros: Double
)

/** Vínculo feito pelo médico a partir do código de acompanhamento mostrado no app da gestante. */
data class VincularGestanteRequest(
    @field:NotNull
    val gestanteId: UUID
)

data class IdadeGestacionalResponse(
    val gestanteId: UUID,
    val dataUltimaMenstruacao: LocalDate,
    val dataProvavelParto: LocalDate,
    val semanas: Long,
    val dias: Long,
    val trimestre: Trimestre,
    val imcInicial: Double,
    val pesoInicialKg: Double = 0.0,
    val alturaMetros: Double = 0.0,
    /** true quando algum médico já vinculou esta gestante ao painel. */
    val medicoVinculado: Boolean = false
)

/**
 * UC07 - "O médico visualiza os dados no painel clínico".
 * Resumo por gestante pro painel do médico: idade gestacional atual,
 * último status de PA (RN02) e se há alerta laboratorial ativo (RN03).
 */
data class GestanteResumoResponse(
    val gestanteId: UUID,
    val nomeGestante: String?,
    val semanas: Long,
    val ultimoNivelRiscoPA: NivelRisco,
    val possuiAlertaLaboratorial: Boolean
)
