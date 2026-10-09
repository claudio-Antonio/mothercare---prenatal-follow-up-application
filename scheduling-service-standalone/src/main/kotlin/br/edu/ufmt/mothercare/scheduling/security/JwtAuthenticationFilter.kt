package br.edu.ufmt.mothercare.scheduling.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * A identidade vem exclusivamente do JWT assinado. Os headers X-Auth-User /
 * X-Auth-Role propagados pelo Gateway NÃO são usados para autenticar: as
 * portas dos microsserviços também ficam acessíveis fora do Gateway (ver
 * docker-compose), e qualquer cliente poderia forjá-los. O Gateway já
 * repassa o Authorization original.
 */
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

        val token = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")?.trim()
        val subject = token?.let { jwtUtil.validarEExtrairSubject(it) }

        if (subject != null) {
            val authorities = jwtUtil.extrairPapel(token)?.let { listOf(SimpleGrantedAuthority(it)) } ?: emptyList()
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(subject, null, authorities)
        }

        filterChain.doFilter(request, response)
    }
}
