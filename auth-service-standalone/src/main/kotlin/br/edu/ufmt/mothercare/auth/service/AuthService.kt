package br.edu.ufmt.mothercare.auth.service

import br.edu.ufmt.mothercare.auth.dto.*
import br.edu.ufmt.mothercare.auth.entity.Usuario
import br.edu.ufmt.mothercare.auth.exception.CredenciaisInvalidasException
import br.edu.ufmt.mothercare.auth.exception.EmailJaCadastradoException
import br.edu.ufmt.mothercare.auth.repository.UsuarioRepository
import br.edu.ufmt.mothercare.auth.security.JwtUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * RF01 - Cadastro de Usuário e emissão de credenciais (login).
 * Fluxo alternativo A1 do UC01: email já cadastrado -> não recria conta,
 * sinaliza para o cliente redirecionar ao login.
 */
@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {

    @Transactional
    fun registrar(request: CadastroRequest): AuthResponse {
        if (usuarioRepository.existsByEmail(request.email)) {
            throw EmailJaCadastradoException(request.email)
        }

        val usuario = usuarioRepository.save(
            Usuario(
                nome = request.nome,
                email = request.email,
                senhaHash = passwordEncoder.encode(request.senha),
                papel = request.papel
            )
        )

        return gerarResposta(usuario)
    }

    fun login(request: LoginRequest): AuthResponse {
        val usuario = usuarioRepository.findByEmail(request.email)
            ?: throw CredenciaisInvalidasException()

        if (!passwordEncoder.matches(request.senha, usuario.senhaHash)) {
            throw CredenciaisInvalidasException()
        }

        return gerarResposta(usuario)
    }

    private fun gerarResposta(usuario: Usuario): AuthResponse {
        val token = jwtUtil.gerarToken(
            subject = usuario.id.toString(),
            claims = mapOf("papel" to usuario.papel.name, "email" to usuario.email)
        )
        return AuthResponse(
            token = token,
            usuario = UsuarioResponse(
                id = usuario.id.toString(),
                nome = usuario.nome,
                email = usuario.email,
                papel = usuario.papel
            )
        )
    }
}
