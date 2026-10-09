package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.dto.NotificacaoResponse
import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.entity.Notificacao
import br.edu.ufmt.mothercare.clinical.entity.TipoNotificacao
import br.edu.ufmt.mothercare.clinical.exception.NotificacaoNaoEncontradaException
import br.edu.ufmt.mothercare.clinical.repository.NotificacaoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * RF08 / seção 8.4 — quem recebe cada alerta. A gestante recebe os alertas
 * da própria gestação; o médico vinculado recebe os alertas laboratoriais
 * (anemia, glicemia) no painel. A notificação só é criada quando o alerta
 * dispara (UC08: sem alteração, sem aviso).
 */
@Service
class NotificacaoService(
    private val repository: NotificacaoRepository,
    private val controleAcesso: ControleAcessoService
) {

    @Transactional
    fun notificarGestante(gestante: Gestante, tipo: TipoNotificacao, mensagem: String) {
        repository.save(Notificacao(destinatarioId = gestante.usuarioId, gestanteId = gestante.id!!, tipo = tipo, mensagem = mensagem))
    }

    @Transactional
    fun notificarMedico(gestante: Gestante, tipo: TipoNotificacao, mensagem: String) {
        val medicoId = gestante.medicoId ?: return
        repository.save(Notificacao(destinatarioId = medicoId, gestanteId = gestante.id!!, tipo = tipo, mensagem = mensagem))
    }

    /** Notificações do usuário autenticado (gestante ou médico), da mais recente para a mais antiga. */
    fun listarDoUsuario(): List<NotificacaoResponse> =
        repository.findByDestinatarioIdOrderByCriadaEmDesc(controleAcesso.usuarioAutenticado().id).map {
            NotificacaoResponse(it.id!!, it.gestanteId, it.tipo, it.mensagem, it.lida, it.criadaEm)
        }

    /** Só o próprio destinatário marca como lida; outro usuário recebe 404 (não revela a existência). */
    @Transactional
    fun marcarComoLida(id: UUID): NotificacaoResponse {
        val usuarioId = controleAcesso.usuarioAutenticado().id
        val notificacao = repository.findById(id)
            .filter { it.destinatarioId == usuarioId }
            .orElseThrow { NotificacaoNaoEncontradaException(id) }
        notificacao.lida = true
        val salva = repository.save(notificacao)
        return NotificacaoResponse(salva.id!!, salva.gestanteId, salva.tipo, salva.mensagem, salva.lida, salva.criadaEm)
    }
}
