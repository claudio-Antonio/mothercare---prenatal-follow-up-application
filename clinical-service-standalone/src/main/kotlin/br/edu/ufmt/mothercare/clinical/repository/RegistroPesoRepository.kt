package br.edu.ufmt.mothercare.clinical.repository

import br.edu.ufmt.mothercare.clinical.entity.RegistroPeso
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistroPesoRepository : JpaRepository<RegistroPeso, UUID> {
    fun findByGestanteIdOrderByRegistradoEmAsc(gestanteId: UUID): List<RegistroPeso>
}
