package br.edu.ufmt.mothercare.clinical.dto

import br.edu.ufmt.mothercare.clinical.entity.TipoNotificacao
import java.time.Instant
import java.util.UUID

/** RF08: notificação exibida para a gestante ou para o médico (GET /notificacoes). */
data class NotificacaoResponse(
    val id: UUID,
    val gestanteId: UUID,
    val tipo: TipoNotificacao,
    val mensagem: String,
    val lida: Boolean,
    val criadaEm: Instant
)
