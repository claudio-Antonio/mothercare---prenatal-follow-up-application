package br.edu.ufmt.mothercare.scheduling.exception

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

    @ExceptionHandler(IntervaloConsultaInvalidoException::class)
    fun handleIntervaloInvalido(ex: IntervaloConsultaInvalidoException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Intervalo de agendamento inválido"))

    @ExceptionHandler(RiscoObstetricoImediatoException::class)
    fun handleRiscoImediato(ex: RiscoObstetricoImediatoException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Agendamento bloqueado por risco obstétrico"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidacao(ex: MethodArgumentNotValidException): ResponseEntity<ErroResponse> {
        val mensagem = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = mensagem))
    }
}
