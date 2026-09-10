package br.edu.ufmt.mothercare.clinical.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

/** UC09 - Monitorar Ganho de Peso. Base para a curva de evolução ponderal e RN05. */
@Entity
@Table(name = "registros_peso")
class RegistroPeso(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false)
    val gestanteId: UUID,

    @Column(nullable = false)
    val pesoKg: Double,

    @Column(nullable = false)
    val registradoEm: LocalDate = LocalDate.now()
)
