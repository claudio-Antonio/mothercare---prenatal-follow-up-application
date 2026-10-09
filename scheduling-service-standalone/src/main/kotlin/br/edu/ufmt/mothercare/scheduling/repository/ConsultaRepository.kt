package br.edu.ufmt.mothercare.scheduling.repository

import br.edu.ufmt.mothercare.scheduling.entity.Consulta
import br.edu.ufmt.mothercare.scheduling.entity.StatusConsulta
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface ConsultaRepository : JpaRepository<Consulta, UUID> {
    fun findByGestanteIdAndStatus(gestanteId: UUID, status: StatusConsulta): List<Consulta>
    fun findByGestanteIdOrderByDataHoraDesc(gestanteId: UUID): List<Consulta>
    fun countByGestanteIdAndStatus(gestanteId: UUID, status: StatusConsulta): Long

    /** RF03: horários ocupados da clínica num dia (vale para todas as gestantes). */
    fun findByDataHoraGreaterThanEqualAndDataHoraLessThanAndStatus(
        inicio: LocalDateTime, fim: LocalDateTime, status: StatusConsulta
    ): List<Consulta>
    fun existsByGestanteIdAndDataHoraBetweenAndStatus(
        gestanteId: UUID, inicio: LocalDateTime, fim: LocalDateTime, status: StatusConsulta
    ): Boolean
}
