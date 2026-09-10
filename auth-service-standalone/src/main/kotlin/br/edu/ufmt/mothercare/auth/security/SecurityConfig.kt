package br.edu.ufmt.mothercare.auth.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

/**
 * O auth-service confia na validação de borda feita pelo API Gateway
 * (Seção 4.2.2 do TCC). Aqui apenas expomos publicamente os endpoints de
 * cadastro/login e desabilitamos sessão (API stateless).
 * Senhas nunca são gravadas em texto puro — uso obrigatório de BCrypt.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/auth/registrar", permitAll)
                authorize("/auth/login", permitAll)
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }
}
