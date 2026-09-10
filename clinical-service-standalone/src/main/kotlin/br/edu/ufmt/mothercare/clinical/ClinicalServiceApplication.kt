package br.edu.ufmt.mothercare.clinical

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Microsserviço de Triagem Clínica (Tabela 1 do TCC).
 * Executa os algoritmos especialistas baseados no Caderno de Atenção
 * Básica nº 32: Regra de Naegele, triagem hipertensiva, triagem
 * laboratorial (anemia/diabetes) e monitoramento de ganho de peso.
 */
@SpringBootApplication
class ClinicalServiceApplication

fun main(args: Array<String>) {
    runApplication<ClinicalServiceApplication>(*args)
}
