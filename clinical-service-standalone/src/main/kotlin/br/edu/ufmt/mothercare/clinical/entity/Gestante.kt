package br.edu.ufmt.mothercare.clinical.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

/**
 * UC01 - Manter Cadastro (DUM, IMC inicial).
 * O campo [usuarioId] referencia o Usuario do auth-service — cada
 * microsserviço mantém apenas o identificador remoto (Database per
 * Service), nunca uma referência de chave estrangeira física entre bancos.
 */
@Entity
@Table(name = "gestantes")
class Gestante(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val usuarioId: UUID,

    @Column(nullable = false)
    val dataUltimaMenstruacao: LocalDate,

    @Column(nullable = false)
    val pesoInicialKg: Double,

    @Column(nullable = false)
    val alturaMetros: Double,

    @Column(nullable = false)
    val criadaEm: LocalDate = LocalDate.now()
) {
    /** IMC pré-gestacional = peso (kg) / altura² (m). Base para RN05. */
    fun imcInicial(): Double = pesoInicialKg / (alturaMetros * alturaMetros)
}
