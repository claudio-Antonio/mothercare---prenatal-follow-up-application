package br.edu.ufmt.mothercare.clinical.exception

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

    @ExceptionHandler(GestanteNaoEncontradaException::class, DocumentoNaoEncontradoException::class)
    fun handleNaoEncontrada(ex: RuntimeException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErroResponse(status = 404, mensagem = ex.message ?: "Não encontrado"))

    @ExceptionHandler(DumInvalidaException::class, IllegalArgumentException::class, FormatoArquivoInvalidoException::class)
    fun handleRequisicaoInvalida(ex: RuntimeException): ResponseEntity<ErroResponse> =
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
