package br.edu.ufmt.mothercare.scheduling.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `os dois 409 devem ter codigos distintos para o app`() {
        val risco = handler.handleRiscoImediato(RiscoObstetricoImediatoException("risco"))
        val intervalo = handler.handleIntervaloInvalido(IntervaloConsultaInvalidoException("intervalo"))

        assertEquals("RISCO_PA", risco.body!!.codigo)
        assertEquals("INTERVALO_MINIMO", intervalo.body!!.codigo)
    }

    @Test
    fun `404 do clinical-service deve chegar como 404 e nao 500`() {
        val resposta = handler.handleErroClinical(HttpClientErrorException(HttpStatus.NOT_FOUND))

        assertEquals(404, resposta.statusCode.value())
        assertEquals("Gestante não encontrada.", resposta.body!!.mensagem)
    }

    @Test
    fun `clinical-service fora do ar deve virar 503`() {
        val resposta = handler.handleClinicalIndisponivel(ResourceAccessException("Connection refused"))

        assertEquals(503, resposta.statusCode.value())
    }
}
