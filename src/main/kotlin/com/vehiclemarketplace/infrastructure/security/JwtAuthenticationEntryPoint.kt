package com.vehiclemarketplace.infrastructure.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        logger.warn("Unauthorized access attempt: ${authException.message}")

        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.writer.write("""
            {
                "error": "Unauthorized",
                "message": "Access denied. Please provide a valid authentication token.",
                "timestamp": "${LocalDateTime.now()}",
                "path": "${request.requestURI}"
            }
        """.trimIndent())
    }
}