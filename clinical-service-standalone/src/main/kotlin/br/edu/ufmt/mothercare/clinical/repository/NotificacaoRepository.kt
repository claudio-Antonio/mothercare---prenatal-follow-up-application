package br.edu.ufmt.mothercare.clinical.repository

import br.edu.ufmt.mothercare.clinical.entity.Notificacao
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificacaoRepository : JpaRepository<Notificacao, UUID> {
    fun findByDestinatarioIdOrderByCriadaEmDesc(destinatarioId: UUID): List<Notificacao>
}
