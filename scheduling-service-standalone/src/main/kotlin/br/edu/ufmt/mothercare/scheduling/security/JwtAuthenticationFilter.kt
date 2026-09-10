package br.edu.ufmt.mothercare.scheduling.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        val usuarioPropagado = request.getHeader("X-Auth-User")

        val subject = when {
            header != null && header.startsWith("Bearer ") ->
                jwtUtil.validarEExtrairSubject(header.removePrefix("Bearer ").trim())
            !usuarioPropagado.isNullOrBlank() -> usuarioPropagado
            else -> null
        }

        if (subject != null) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(subject, null, emptyList())
        }

        filterChain.doFilter(request, response)
    }
}
