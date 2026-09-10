package br.edu.ufmt.mothercare.auth.repository

import br.edu.ufmt.mothercare.auth.entity.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UsuarioRepository : JpaRepository<Usuario, UUID> {
    fun findByEmail(email: String): Usuario?
    fun existsByEmail(email: String): Boolean
}
