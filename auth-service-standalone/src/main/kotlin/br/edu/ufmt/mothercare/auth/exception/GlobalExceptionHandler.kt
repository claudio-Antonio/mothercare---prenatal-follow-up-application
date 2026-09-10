package br.edu.ufmt.mothercare.auth.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidacao(ex: MethodArgumentNotValidException): ResponseEntity<ErroResponse> {
        val mensagem = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = mensagem))
    }
}
