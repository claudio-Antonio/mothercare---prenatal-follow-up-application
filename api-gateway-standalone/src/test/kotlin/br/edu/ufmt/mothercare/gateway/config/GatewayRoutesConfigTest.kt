package br.edu.ufmt.mothercare.gateway.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Flux

/**
 * Garante que cada caminho público do Gateway chega ao microsserviço
 * certo — e que endpoints internos (serviço-a-serviço) não são roteados.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "mothercare.jwt.secret=segredo-de-teste-com-pelo-menos-32-bytes-de-tamanho",
        "mothercare.routes.auth-service=http://auth:8081",
        "mothercare.routes.clinical-service=http://clinical:8082",
        "mothercare.routes.scheduling-service=http://scheduling:8083"
    ]
)
class GatewayRoutesConfigTest {

    @Autowired
    private lateinit var routeLocator: RouteLocator

    private fun rotaPara(method: String, path: String): String? {
        val request = when (method) {
            "POST" -> MockServerHttpRequest.post(path).build()
            else -> MockServerHttpRequest.get(path).build()
        }
        val exchange = MockServerWebExchange.from(request)
        return routeLocator.routes
            .concatMap { rota -> Flux.from(rota.predicate.apply(exchange)).filter { it }.map { rota.id } }
            .next()
            .block()
    }

    @ParameterizedTest
    @CsvSource(
        "POST, /api/auth/registrar, auth-service",
        "POST, /api/auth/login, auth-service",
        "POST, /api/gestantes, clinical-service",
        "GET, /api/gestantes/medico/minhas-gestantes, clinical-service",
        "POST, /api/checkin/pressao, clinical-service",
        "POST, /api/exames, clinical-service",
        "GET, /api/peso/123/grafico, clinical-service",
        "GET, /api/prontuario/documentos/123, clinical-service",
        "POST, /api/consultas, scheduling-service"
    )
    fun `deve rotear caminhos publicos para o microsservico correto`(method: String, path: String, esperado: String) {
        assertEquals(esperado, rotaPara(method, path))
    }

    @ParameterizedTest
    @CsvSource(
        "POST, /api/auth/usuarios/lote",
        "GET, /api/desconhecido"
    )
    fun `nao deve rotear endpoints internos ou desconhecidos`(method: String, path: String) {
        assertNull(rotaPara(method, path))
    }
}
