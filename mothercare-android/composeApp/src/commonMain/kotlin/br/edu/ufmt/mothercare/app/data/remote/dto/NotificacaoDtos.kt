package br.edu.ufmt.mothercare.app.data.remote.dto

import kotlinx.serialization.Serializable

/** RF08: notificação in-app (gestante ou médico). tipo: PRESSAO_CRITICA | ANEMIA | DIABETES_GESTACIONAL | GANHO_PESO_EXCESSIVO | LEMBRETE_CONSULTA */
@Serializable
data class NotificacaoResponse(
    val id: String,
    val gestanteId: String,
    val tipo: String,
    val mensagem: String,
    val lida: Boolean,
    val criadaEm: String
)
