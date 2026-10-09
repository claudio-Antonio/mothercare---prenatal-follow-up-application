package br.edu.ufmt.mothercare.clinical.security

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
 * docker-compose), e qualquer cliente poderia forjar "X-Auth-Role: MEDICO".
 * O Gateway e o scheduling-service já repassam o Authorization original.
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

        var subject: String? = null
        var papel: String? = null

        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            subject = jwtUtil.validarEExtrairSubject(token)
            papel = jwtUtil.extrairPapel(token)
        }

        if (subject != null) {
            val authorities = papel?.let { listOf(SimpleGrantedAuthority(it)) } ?: emptyList()
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(subject, null, authorities)
        }

        filterChain.doFilter(request, response)
    }
}
