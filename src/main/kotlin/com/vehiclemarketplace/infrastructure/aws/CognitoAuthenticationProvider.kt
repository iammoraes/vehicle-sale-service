package com.vehiclemarketplace.infrastructure.aws

import kotlinx.coroutines.runBlocking
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType

@Component
class CognitoAuthenticationProvider(
    private val awsCognitoService: AwsCognitoService
) : AuthenticationProvider {
    
    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name
        val password = authentication.credentials.toString()

        try {
            val authResult: AuthenticationResultType = runBlocking { 
                awsCognitoService.authenticate(username, password)
            }
            val userDetails: UserDetails = awsCognitoService.loadUserByUsername(username)

            return UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            ).apply {
                details = CognitoAuthenticationDetails(authResult)
            }
        } catch (e: Exception) {
            throw BadCredentialsException(e.message)
        }
    }

    override fun supports(authentication: Class<*>): Boolean {
        return UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
    }

    class CognitoAuthenticationDetails(
        val authenticationResult: AuthenticationResultType
    ) {
        val accessToken: String = authenticationResult.accessToken()
        val refreshToken: String? = authenticationResult.refreshToken()
        val idToken: String? = authenticationResult.idToken()
        val expiresIn: Int = authenticationResult.expiresIn()
        val tokenType: String = authenticationResult.tokenType()
    }
}
