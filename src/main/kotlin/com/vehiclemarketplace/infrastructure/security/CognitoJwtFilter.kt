package com.vehiclemarketplace.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse

@Component
class CognitoJwtFilter(
    private val awsCognitoService: AwsCognitoService,
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader = request.getHeader("Authorization")
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val jwtToken = authHeader.substring(7)
                
                if (isCognitoToken(jwtToken)) {
                    processCognitoToken(jwtToken, request)
                } else {
                    processStandardJwtToken(jwtToken, request)
                }
            }
        } catch (e: Exception) {
            logger.error(e.message)
        }
        
        filterChain.doFilter(request, response)
    }

    private fun isCognitoToken(token: String): Boolean {
        return token.length > 1000
    }

    private fun processCognitoToken(token: String, request: HttpServletRequest) {
        try {
            val username = getUsernameFromCognitoToken(token)
            
            if (username != null && SecurityContextHolder.getContext().authentication == null) {
                val userDetails = awsCognitoService.loadUserByUsername(username)
                
                val authToken = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )
                
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        } catch (e: Exception) {
            logger.error(e.message)
        }
    }

    private fun getUsernameFromCognitoToken(token: String): String? {
        return try {
            val getUserRequest = GetUserRequest.builder()
                .accessToken(token)
                .build()
            
            val userResponse = awsCognitoService.cognitoClient.getUser(getUserRequest)
            userResponse.username()
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }

    private fun processStandardJwtToken(token: String, request: HttpServletRequest) {
        val username = jwtUtil.getUsernameFromToken(token)
        
        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            if (jwtUtil.validateToken(token)) {
                val userDetails = awsCognitoService.loadUserByUsername(username)
                
                val authToken = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )
                
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }
    }
}
