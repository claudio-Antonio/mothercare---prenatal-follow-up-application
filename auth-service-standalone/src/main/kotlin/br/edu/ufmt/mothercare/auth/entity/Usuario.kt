package br.edu.ufmt.mothercare.auth.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Entidade de autenticação. Mantém apenas os dados de credencial/identidade;
 * dados clínicos (DUM, peso, altura) pertencem ao clinical-service, seguindo
 * o padrão Database per Service (Seção 4.2.2 do TCC) — nenhum microsserviço
 * acessa diretamente o schema de outro.
 */
@Entity
@Table(name = "usuarios")
class Usuario(
    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(nullable = false)
    var nome: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var senhaHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var papel: Papel,

    @Column(nullable = false)
    var criadoEm: Instant = Instant.now()
)
