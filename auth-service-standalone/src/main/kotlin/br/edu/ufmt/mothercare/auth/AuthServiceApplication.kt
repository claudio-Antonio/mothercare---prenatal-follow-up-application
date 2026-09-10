package br.edu.ufmt.mothercare.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Microsserviço de Autenticação (Tabela 1 do TCC).
 * Gerencia o ciclo de vida de acesso, cadastro de perfis (Gestante/Médico)
 * e a emissão centralizada de credenciais de segurança (JWT).
 */
@SpringBootApplication
class AuthServiceApplication

fun main(args: Array<String>) {
    runApplication<AuthServiceApplication>(*args)
}
