package br.edu.ufmt.mothercare.clinical.repository

import br.edu.ufmt.mothercare.clinical.entity.RegistroPressaoArterial
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistroPressaoArterialRepository : JpaRepository<RegistroPressaoArterial, UUID> {
    fun findByGestanteIdOrderByRegistradoEmDesc(gestanteId: UUID): List<RegistroPressaoArterial>
}
