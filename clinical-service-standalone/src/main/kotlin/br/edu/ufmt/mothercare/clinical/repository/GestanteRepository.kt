package br.edu.ufmt.mothercare.clinical.repository

import br.edu.ufmt.mothercare.clinical.entity.Gestante
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GestanteRepository : JpaRepository<Gestante, UUID> {
    fun findByUsuarioId(usuarioId: UUID): Gestante?
}
