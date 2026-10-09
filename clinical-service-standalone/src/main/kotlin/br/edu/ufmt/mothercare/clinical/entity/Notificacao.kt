package br.edu.ufmt.mothercare.clinical.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** RF08 / seção 8.4: tipo do alerta que originou a notificação. */
enum class TipoNotificacao {
    PRESSAO_CRITICA,
    ANEMIA,
    DIABETES_GESTACIONAL,
    GANHO_PESO_EXCESSIVO,
    LEMBRETE_CONSULTA
}

/**
 * Notificação in-app para gestante ou médico. [destinatarioId] é o id do
 * usuário no auth-service (usuarioId da gestante ou id do médico), o mesmo
 * valor que vem no sub do JWT.
 *
 * Propositalmente NÃO é uma `data class` (mesmo motivo das demais entidades JPA).
 */
@Entity
@Table(name = "notificacoes")
class Notificacao(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false)
    val destinatarioId: UUID,

    @Column(nullable = false)
    val gestanteId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipo: TipoNotificacao,

    @Column(nullable = false, length = 500)
    val mensagem: String,

    @Column(nullable = false)
    var lida: Boolean = false,

    @Column(nullable = false)
    val criadaEm: Instant = Instant.now()
)
