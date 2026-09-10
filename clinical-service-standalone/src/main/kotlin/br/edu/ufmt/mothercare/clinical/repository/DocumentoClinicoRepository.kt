package br.edu.ufmt.mothercare.clinical.repository

import br.edu.ufmt.mothercare.clinical.entity.DocumentoClinico
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentoClinicoRepository : JpaRepository<DocumentoClinico, UUID> {
    /** Doc. de regras de negócio: exames organizados "em ordem cronológica reversa por trimestre". */
    fun findByGestanteIdOrderByEnviadoEmDesc(gestanteId: UUID): List<DocumentoClinico>
}
