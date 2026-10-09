package br.edu.ufmt.mothercare.clinical.repository

import br.edu.ufmt.mothercare.clinical.entity.DocumentoClinico
import br.edu.ufmt.mothercare.clinical.entity.TipoExame
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/** Projeção sem o binário (@Lob) — o checklist só precisa saber o que foi enviado e quando. */
interface DocumentoResumo {
    val categoria: String
    val tipoExameRastreado: TipoExame?
    val enviadoEm: Instant
}

interface DocumentoClinicoRepository : JpaRepository<DocumentoClinico, UUID> {
    /** Doc. de regras de negócio: exames organizados "em ordem cronológica reversa por trimestre". */
    fun findByGestanteIdOrderByEnviadoEmDesc(gestanteId: UUID): List<DocumentoClinico>
    fun findResumoByGestanteId(gestanteId: UUID): List<DocumentoResumo>
}
