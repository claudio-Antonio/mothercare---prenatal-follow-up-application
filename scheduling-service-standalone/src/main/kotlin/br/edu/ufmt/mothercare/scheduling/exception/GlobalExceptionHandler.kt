package br.edu.ufmt.mothercare.scheduling.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.time.Instant

/**
 * [codigo] diferencia os dois 409 do agendamento para o app:
 * RISCO_PA (RF05, procurar atendimento) x INTERVALO_MINIMO (RN04, escolher outra data).
 */
data class ErroResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val mensagem: String,
    val codigo: String? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IntervaloConsultaInvalidoException::class)
    fun handleIntervaloInvalido(ex: IntervaloConsultaInvalidoException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Intervalo de agendamento inválido", codigo = CODIGO_INTERVALO))

    @ExceptionHandler(RiscoObstetricoImediatoException::class)
    fun handleRiscoImediato(ex: RiscoObstetricoImediatoException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Agendamento bloqueado por risco obstétrico", codigo = CODIGO_RISCO))

    @ExceptionHandler(HorarioIndisponivelException::class)
    fun handleHorarioIndisponivel(ex: HorarioIndisponivelException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Horário indisponível", codigo = CODIGO_SEM_HORARIO))

    @ExceptionHandler(AgendamentoAposDppException::class)
    fun handleAposDpp(ex: AgendamentoAposDppException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErroResponse(status = 422, mensagem = ex.message ?: "Agendamento após a DPP", codigo = CODIGO_APOS_DPP))

    @ExceptionHandler(ConsultaNaoEncontradaException::class)
    fun handleNaoEncontrada(ex: ConsultaNaoEncontradaException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErroResponse(status = 404, mensagem = ex.message ?: "Consulta não encontrada"))

    @ExceptionHandler(ConsultaNaoCancelavelException::class)
    fun handleNaoCancelavel(ex: ConsultaNaoCancelavelException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Consulta não pode ser cancelada", codigo = CODIGO_NAO_CANCELAVEL))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidacao(ex: MethodArgumentNotValidException): ResponseEntity<ErroResponse> {
        val mensagem = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = mensagem))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleCorpoIlegivel(ex: HttpMessageNotReadableException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = "Corpo inválido. Use dataHoraDesejada no formato yyyy-MM-ddTHH:mm."))

    /** 4xx do clinical-service (gestante inexistente, sem acesso, token expirado) — antes virava 500. */
    @ExceptionHandler(HttpClientErrorException::class)
    fun handleErroClinical(ex: HttpClientErrorException): ResponseEntity<ErroResponse> {
        val status = ex.statusCode.value()
        val mensagem = when (status) {
            401 -> "Sessão expirada. Faça login novamente."
            403 -> "Você não tem acesso aos dados desta gestante."
            404 -> "Gestante não encontrada."
            else -> "Não foi possível validar o agendamento no serviço clínico."
        }
        return ResponseEntity.status(status).body(ErroResponse(status = status, mensagem = mensagem))
    }

    @ExceptionHandler(ResourceAccessException::class, HttpServerErrorException::class)
    fun handleClinicalIndisponivel(ex: Exception): ResponseEntity<ErroResponse> {
        log.warn("Falha ao consultar o clinical-service: {}", ex.message)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErroResponse(status = 503, mensagem = "Serviço clínico indisponível. Tente novamente em instantes."))
    }

    companion object {
        const val CODIGO_RISCO = "RISCO_PA"
        const val CODIGO_INTERVALO = "INTERVALO_MINIMO"
        const val CODIGO_NAO_CANCELAVEL = "NAO_CANCELAVEL"
        const val CODIGO_SEM_HORARIO = "SEM_HORARIO"
        const val CODIGO_APOS_DPP = "APOS_DPP"
    }
}
