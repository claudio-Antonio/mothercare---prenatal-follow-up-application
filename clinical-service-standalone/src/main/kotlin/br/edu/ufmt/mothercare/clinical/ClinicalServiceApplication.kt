package br.edu.ufmt.mothercare.clinical

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@SpringBootApplication
class ClinicalServiceApplication {
    /** Timeouts explícitos: sem eles, um serviço vizinho travado prende a thread até o gateway estourar. */
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(2_000)
            setReadTimeout(3_000)
        }
    )
}

fun main(args: Array<String>) {
    runApplication<ClinicalServiceApplication>(*args)
}