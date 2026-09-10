package br.edu.ufmt.mothercare.scheduling.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * UC03 - Agendar Consulta. RN04: a partir da 28ª semana (8º mês), o
 * agendamento passa a ser semanal em vez de mensal. O documento de
 * regras de negócio também exige o mínimo de 6 consultas de pré-natal.
 *
 * Propositalmente NÃO é uma `data class` — mesmo motivo do auth-service
 * e clinical-service: equals/hashCode/copy gerados conflitam com o
 * gerenciamento de identidade do JPA/Hibernate.
 */
@Entity
@Table(name = "consultas")
class Consulta(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false)
    val gestanteId: UUID,

    @Column(nullable = false)
    val dataHora: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: StatusConsulta = StatusConsulta.AGENDADA,

    @Column(nullable = false)
    val semanaGestacionalNoAgendamento: Int,

    @Column(nullable = false)
    val criadaEm: LocalDateTime = LocalDateTime.now()
)
