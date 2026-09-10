package br.edu.ufmt.mothercare.scheduling

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.client.RestTemplate
import org.springframework.context.annotation.Bean

/**
 * Microsserviço de Agendamento (Tabela 1 do TCC).
 * Gerencia, de forma isolada, os fluxos de cronogramas e marcação de
 * consultas pré-natais (RN04).
 */
@SpringBootApplication
class SchedulingServiceApplication {
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}

fun main(args: Array<String>) {
    runApplication<SchedulingServiceApplication>(*args)
}
