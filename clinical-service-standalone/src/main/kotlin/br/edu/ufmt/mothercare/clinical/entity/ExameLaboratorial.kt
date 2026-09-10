package br.edu.ufmt.mothercare.clinical.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * UC07/UC08 - Gerenciar Prontuário Digital e Emitir Alertas.
 * RN03: Hb < 11 g/dL (anemia) e Glicemia de Jejum >= 92 mg/dL (diabetes
 * gestacional) disparam alerta automático.
 */
@Entity
@Table(name = "exames_laboratoriais")
class ExameLaboratorial(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false)
    val gestanteId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipo: TipoExame,

    @Column(nullable = false)
    val valor: Double,

    @Column(nullable = false)
    val alertaDisparado: Boolean,

    /** Referência ao laudo digitalizado, se o app enviou o arquivo junto com o valor numérico. */
    @Column
    val documentoId: UUID? = null,

    @Column(nullable = false)
    val registradoEm: Instant = Instant.now()
)
