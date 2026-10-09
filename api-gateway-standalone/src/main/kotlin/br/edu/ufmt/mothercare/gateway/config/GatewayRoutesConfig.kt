package br.edu.ufmt.mothercare.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Roteamento declarativo do Gateway para os três microsserviços internos
 * (Tabela 1 do TCC): Autenticação, Triagem Clínica e Agendamento.
 *
 * O app Android conhece apenas o endereço único do Gateway, nunca as
 * URLs individuais de cada microsserviço (Seção 4.4.2 do TCC).
 *
 * As URIs de destino vêm de propriedades (`mothercare.routes.*`), não de
 * `lb://` — este projeto não tem um service discovery (Eureka/Consul)
 * configurado, então balanceamento via nome lógico de serviço não
 * funcionaria. Em desenvolvimento local, essas propriedades apontam para
 * `localhost:<porta>`; no docker-compose, são sobrescritas para o nome
 * do serviço no Compose (resolvido pela rede interna do Docker).
 *
 * Do auth-service só são expostos cadastro e login. `/auth/usuarios/lote`
 * é exclusivo da comunicação serviço-a-serviço (clinical → auth) e não
 * pode ser alcançado pelo app — por isso a rota lista os caminhos
 * explicitamente em vez de usar um curinga sobre /api/auth.
 */
@Configuration
class GatewayRoutesConfig(
    @Value("\${mothercare.routes.auth-service}") private val authServiceUri: String,
    @Value("\${mothercare.routes.clinical-service}") private val clinicalServiceUri: String,
    @Value("\${mothercare.routes.scheduling-service}") private val schedulingServiceUri: String
) {

    @Bean
    fun rotas(builder: RouteLocatorBuilder): RouteLocator =
        builder.routes()
            .route("auth-service") { r ->
                r.path("/api/auth/registrar", "/api/auth/login")
                    .filters { f -> f.rewritePath("/api/auth/(?<segment>.*)", "/auth/\${segment}") }
                    .uri(authServiceUri)
            }
            .route("clinical-service") { r ->
                r.path(
                    "/api/gestantes/**", "/api/checkin/**", "/api/exames/**",
                    "/api/peso/**", "/api/prontuario/**", "/api/notificacoes/**"
                )
                    .filters { f -> f.rewritePath("/api/(?<segment>.*)", "/\${segment}") }
                    .uri(clinicalServiceUri)
            }
            .route("scheduling-service") { r ->
                r.path("/api/consultas/**")
                    .filters { f -> f.rewritePath("/api/(?<segment>.*)", "/\${segment}") }
                    .uri(schedulingServiceUri)
            }
            .build()
}
