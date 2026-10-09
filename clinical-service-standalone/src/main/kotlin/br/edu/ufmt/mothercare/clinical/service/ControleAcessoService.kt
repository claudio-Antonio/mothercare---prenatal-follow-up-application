package br.edu.ufmt.mothercare.clinical.service

import br.edu.ufmt.mothercare.clinical.entity.Gestante
import br.edu.ufmt.mothercare.clinical.exception.AcessoNegadoException
import br.edu.ufmt.mothercare.clinical.exception.GestanteNaoEncontradaException
import br.edu.ufmt.mothercare.clinical.repository.GestanteRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Checagem de posse do recurso: o JWT só prova QUEM é o usuário; aqui se
 * decide se ele pode ver/alterar os dados de uma gestante específica.
 * - GESTANTE: apenas o próprio cadastro (gestante.usuarioId == sub do token).
 * - MEDICO: apenas gestantes vinculadas a ele (gestante.medicoId == sub), e só leitura:
 *   check-in, exame, peso, laudo e cancelamento de consulta são registrados pela gestante.
 */
@Service
class ControleAcessoService(
    private val gestanteRepository: GestanteRepository
) {
    data class UsuarioAutenticado(val id: UUID, val papel: String?)

    fun usuarioAutenticado(): UsuarioAutenticado {
        val autenticacao = SecurityContextHolder.getContext().authentication
            ?: throw AcessoNegadoException("Usuário não autenticado")
        val id = try {
            UUID.fromString(autenticacao.name)
        } catch (ex: IllegalArgumentException) {
            throw AcessoNegadoException("Identidade do token inválida")
        }
        return UsuarioAutenticado(id, autenticacao.authorities.firstOrNull()?.authority)
    }

    /**
     * Carrega a gestante e falha com 403 se o usuário do token não tem acesso a ela.
     * [escrita] = true restringe à própria gestante (o médico vinculado só lê).
     */
    fun verificarAcesso(gestanteId: UUID, escrita: Boolean = false): Gestante {
        val gestante = gestanteRepository.findById(gestanteId).orElseThrow { GestanteNaoEncontradaException(gestanteId) }
        val usuario = usuarioAutenticado()
        val permitido = when (usuario.papel) {
            PAPEL_GESTANTE -> gestante.usuarioId == usuario.id
            PAPEL_MEDICO -> !escrita && gestante.medicoId == usuario.id
            else -> false
        }
        if (!permitido) throw AcessoNegadoException()
        return gestante
    }

    /**
     * UC07 (Gestante e Médico): registro de exame laboratorial, laudo ou documento do prontuário.
     * A gestante registra no próprio cadastro; o médico vinculado também pode registrar.
     * Check-in de PA e peso continuam restritos à gestante (verificarAcesso(escrita = true)).
     */
    fun verificarRegistroClinico(gestanteId: UUID): Gestante {
        val gestante = gestanteRepository.findById(gestanteId).orElseThrow { GestanteNaoEncontradaException(gestanteId) }
        val usuario = usuarioAutenticado()
        val permitido = when (usuario.papel) {
            PAPEL_GESTANTE -> gestante.usuarioId == usuario.id
            PAPEL_MEDICO -> gestante.medicoId == usuario.id
            else -> false
        }
        if (!permitido) throw AcessoNegadoException()
        return gestante
    }

    /** Rotas /gestantes/me: só fazem sentido para a gestante; devolve o id do usuário. */
    fun exigirGestante(): UUID {
        val usuario = usuarioAutenticado()
        if (usuario.papel != PAPEL_GESTANTE) throw AcessoNegadoException("Operação disponível apenas para a gestante")
        return usuario.id
    }

    /** Cadastro clínico: uma gestante só pode criar o cadastro do próprio usuário. */
    fun verificarProprioUsuario(usuarioId: UUID) {
        val usuario = usuarioAutenticado()
        if (usuario.papel != PAPEL_GESTANTE || usuario.id != usuarioId) {
            throw AcessoNegadoException("O cadastro clínico só pode ser criado pela própria gestante")
        }
    }

    companion object {
        const val PAPEL_GESTANTE = "GESTANTE"
        const val PAPEL_MEDICO = "MEDICO"
    }
}
