package br.edu.ufmt.mothercare.app.data.remote.dto
import kotlinx.serialization.Serializable

@Serializable
data class CadastroGestanteRequest(
    val usuarioId: String,
    val dataUltimaMenstruacao: String,
    val pesoInicialKg: Double,
    val alturaMetros: Double
)

/** Painel do médico: vínculo pelo código de acompanhamento (gestanteId) que a gestante vê no app. */
@Serializable
data class VincularGestanteRequest(
    val gestanteId: String
)

@Serializable
data class IdadeGestacionalResponse(
    val gestanteId: String,
    val dataUltimaMenstruacao: String,
    val dataProvavelParto: String,
    val semanas: Long,
    val dias: Long,
    val trimestre: String,
    val imcInicial: Double,
    val pesoInicialKg: Double = 0.0,
    val alturaMetros: Double = 0.0,
    val medicoVinculado: Boolean = false
)

/** Perfil (PUT /gestantes/me): correção dos dados clínicos iniciais. */
@Serializable
data class AtualizarGestanteRequest(
    val dataUltimaMenstruacao: String,
    val pesoInicialKg: Double,
    val alturaMetros: Double
)
