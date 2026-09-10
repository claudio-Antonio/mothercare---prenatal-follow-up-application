package br.edu.ufmt.mothercare.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Ponto de entrada único do ecossistema Mother Care (Seção 4.2.2 do TCC,
 * Tabela 1: "Atuar como porta de entrada única para o cliente,
 * interceptar requisições, validar tokens de segurança (JWT) e efetuar
 * o roteamento de rede").
 *
 * O app Android conhece apenas este endereço — nunca as URLs internas
 * de auth-service, clinical-service ou scheduling-service (Seção 4.4.2).
 */
@SpringBootApplication
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
