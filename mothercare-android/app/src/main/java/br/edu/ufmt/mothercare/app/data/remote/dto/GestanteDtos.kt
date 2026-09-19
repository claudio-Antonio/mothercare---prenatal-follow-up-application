package br.edu.ufmt.mothercare.app.data.remote.dto

data class CadastroGestanteRequest(
    val usuarioId: String,
    val dataUltimaMenstruacao: String,
    val pesoInicialKg: Double,
    val alturaMetros: Double
)

data class IdadeGestacionalResponse(
    val gestanteId: String,
    val dataUltimaMenstruacao: String,
    val dataProvavelParto: String,
    val semanas: Long,
    val dias: Long,
    val trimestre: String,
    val imcInicial: Double
)
