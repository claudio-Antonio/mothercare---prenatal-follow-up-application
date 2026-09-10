package br.edu.ufmt.mothercare.clinical.repository

import br.edu.ufmt.mothercare.clinical.entity.ExameLaboratorial
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExameLaboratorialRepository : JpaRepository<ExameLaboratorial, UUID> {
    fun findByGestanteIdOrderByRegistradoEmDesc(gestanteId: UUID): List<ExameLaboratorial>
}
