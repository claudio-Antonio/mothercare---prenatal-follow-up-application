package br.edu.ufmt.mothercare.auth.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ErroResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val mensagem: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException::class)
    fun handleEmailJaCadastrado(ex: EmailJaCadastradoException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Email já cadastrado"))

    @ExceptionHandler(CredenciaisInvalidasException::class)
    fun handleCredenciaisInvalidas(ex: CredenciaisInvalidasException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErroResponse(status = 401, mensagem = ex.message ?: "Credenciais inválidas"))

    @ExceptionHandler(CadastroMedicoNaoAutorizadoException::class, AcessoNegadoException::class)
    fun handleProibido(ex: RuntimeException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErroResponse(status = 403, mensagem = ex.message ?: "Acesso negado"))

    /** Ex.: papel desconhecido ("papel": "ADMIN") ou JSON mal formado. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleCorpoIlegivel(ex: HttpMessageNotReadableException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = "Corpo da requisição inválido"))

    /** Ex.: UUID mal formado em /auth/usuarios/lote. */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleRequisicaoInvalida(ex: IllegalArgumentException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = ex.message ?: "Requisição inválida"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidacao(ex: MethodArgumentNotValidException): ResponseEntity<ErroResponse> {
        val mensagem = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = mensagem))
    }
}
