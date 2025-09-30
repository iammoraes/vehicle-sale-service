package com.vehiclemarketplace.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtRequestFilter(
    private val jwtUtil: JwtUtil,
    private val auditService: AuditService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val requestTokenHeader = request.getHeader("Authorization")

        var username: String? = null
        var userId: String? = null
        var jwtToken: String? = null

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7)
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken)
                userId = jwtUtil.getUserIdFromToken(jwtToken)
            } catch (e: Exception) {
                logger.warn("Unable to get JWT Token or token is expired", e)
            }
        }

        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            if (jwtUtil.validateToken(jwtToken!!)) {
                val roles = jwtUtil.getRolesFromToken(jwtToken)
                val authorities = roles.map { SimpleGrantedAuthority(it) }

                val authToken = UsernamePasswordAuthenticationToken(
                    username, null, authorities
                ).apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }

                SecurityContextHolder.getContext().authentication = authToken

                userId?.let { uid ->
                    runBlocking {
                        auditService.logAccess(
                            userId = uid,
                            action = "ACCESS",
                            entityType = "API",
                            entityId = request.requestURI,
                            userIp = getClientIpAddress(request),
                            userAgent = request.getHeader("User-Agent")
                        )
                    }
                }
            }
        }

        chain.doFilter(request, response)
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedForHeader = request.getHeader("X-Forwarded-For")
        return when {
            xForwardedForHeader != null && xForwardedForHeader.isNotEmpty() -> {
                xForwardedForHeader.split(",")[0].trim()
            }
            else -> request.remoteAddr
        }
    }
}
