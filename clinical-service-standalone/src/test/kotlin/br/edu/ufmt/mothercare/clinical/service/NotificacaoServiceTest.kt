package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.entity.Notificacao
import br.edu.ufmt.mothercare.clinical.entity.TipoNotificacao
import br.edu.ufmt.mothercare.clinical.exception.NotificacaoNaoEncontradaException
import br.edu.ufmt.mothercare.clinical.repository.NotificacaoRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

/** RF08 — só o próprio destinatário lê/marca a notificação; outro usuário recebe 404. */
class NotificacaoServiceTest {

    private val repository = mockk<NotificacaoRepository>()
    private val controleAcesso = mockk<ControleAcessoService>()
    private val service = NotificacaoService(repository, controleAcesso)

    private val donoId = UUID.randomUUID()
    private val notificacao = Notificacao(
        id = UUID.randomUUID(),
        destinatarioId = donoId,
        gestanteId = UUID.randomUUID(),
        tipo = TipoNotificacao.ANEMIA,
        mensagem = "Alerta de anemia"
    )

    @Test
    fun `destinatario marca a propria notificacao como lida`() {
        every { controleAcesso.usuarioAutenticado() } returns ControleAcessoService.UsuarioAutenticado(donoId, "GESTANTE")
        every { repository.findById(notificacao.id!!) } returns Optional.of(notificacao)
        every { repository.save(any()) } answers { firstArg() }

        val resposta = service.marcarComoLida(notificacao.id!!)

        assertTrue(resposta.lida)
    }

    @Test
    fun `outro usuario nao consegue marcar a notificacao (404, sem revelar existencia)`() {
        every { controleAcesso.usuarioAutenticado() } returns
            ControleAcessoService.UsuarioAutenticado(UUID.randomUUID(), "MEDICO")
        every { repository.findById(notificacao.id!!) } returns Optional.of(notificacao)

        assertThrows(NotificacaoNaoEncontradaException::class.java) { service.marcarComoLida(notificacao.id!!) }
    }
}
