package br.edu.ufmt.mothercare.clinical.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** UC04 - Realizar Check-in de Saúde (PA). Base para a triagem hipertensiva (RN02). */
@Entity
@Table(name = "registros_pressao_arterial")
class RegistroPressaoArterial(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false)
    val gestanteId: UUID,

    @Column(nullable = false)
    val sistolica: Int,

    @Column(nullable = false)
    val diastolica: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val nivelRisco: NivelRisco,

    @Column(nullable = false)
    val registradoEm: Instant = Instant.now()
)
