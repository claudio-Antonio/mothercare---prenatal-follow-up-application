package br.edu.ufmt.mothercare.clinical.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import java.time.Instant

data class ErroResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val mensagem: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(GestanteNaoEncontradaException::class, DocumentoNaoEncontradoException::class, NotificacaoNaoEncontradaException::class)
    fun handleNaoEncontrada(ex: RuntimeException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErroResponse(status = 404, mensagem = ex.message ?: "Não encontrado"))

    @ExceptionHandler(GestanteJaCadastradaException::class, GestanteJaVinculadaException::class)
    fun handleConflito(ex: RuntimeException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErroResponse(status = 409, mensagem = ex.message ?: "Cadastro já existente"))

    @ExceptionHandler(AcessoNegadoException::class)
    fun handleAcessoNegado(ex: AcessoNegadoException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErroResponse(status = 403, mensagem = ex.message ?: "Acesso negado"))

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

    /** JSON mal formado, data fora do padrão yyyy-MM-dd, enum desconhecido etc. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleCorpoIlegivel(ex: HttpMessageNotReadableException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = "Corpo da requisição inválido. Verifique os campos e o formato das datas (yyyy-MM-dd)."))

    /** Ex.: gestanteId na URL que não é um UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTipoInvalido(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = "Parâmetro '${ex.name}' inválido"))

    @ExceptionHandler(MissingServletRequestParameterException::class, MissingServletRequestPartException::class)
    fun handleParametroAusente(ex: Exception): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErroResponse(status = 400, mensagem = ex.message ?: "Parâmetro obrigatório ausente"))

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleArquivoGrande(ex: MaxUploadSizeExceededException): ResponseEntity<ErroResponse> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ErroResponse(status = 413, mensagem = "Arquivo maior que o limite de 10 MB"))
}
