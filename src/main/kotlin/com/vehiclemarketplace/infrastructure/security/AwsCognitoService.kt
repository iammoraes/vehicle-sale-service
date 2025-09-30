package com.vehiclemarketplace.infrastructure.security

import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.*
import java.util.*

@Service
class AwsCognitoService(
    internal val cognitoClient: CognitoIdentityProviderClient,
    private val userPoolId: String,
    private val clientId: String,
    private val clientSecret: String,
    private val auditService: AuditService
) : UserDetailsService {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun authenticate(username: String, password: String): AuthenticationResultType {
        try {
            val authParams = mapOf(
                "USERNAME" to username,
                "PASSWORD" to password
            )

            val request = AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .authParameters(authParams)
                .build()

            val response = cognitoClient.adminInitiateAuth(request)
            val authResult = response.authenticationResult()

            auditService.logAccess(
                userId = getUserIdByUsername(username) ?: "",
                action = "LOGIN",
                entityType = "AUTH",
                entityId = UUID.randomUUID().toString()
            )

            return authResult
        } catch (e: Exception) {
            logger.error("Authenticate in Cognito: ${e.message}", e)
            
            when (e) {
                is UserNotFoundException -> throw UsernameNotFoundException("User not found")
                is NotAuthorizedException -> throw SecurityException("Not authorized")
                is UserNotConfirmedException -> throw SecurityException("User not confirmed")
                else -> throw SecurityException("Authenticate in Cognito: ${e.message}")
            }
        }
    }

    fun registerUser(username: String, password: String, email: String, phone: String): String {
        try {
            val userAttributes = listOf(
                AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build(),
                AttributeType.builder()
                    .name("phone_number")
                    .value(phone)
                    .build(),
                AttributeType.builder()
                    .name("email_verified")
                    .value("true")
                    .build()
            )

            val request = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .temporaryPassword(password)
                .userAttributes(userAttributes)
                .messageAction(MessageActionType.SUPPRESS)
                .build()

            val response = cognitoClient.adminCreateUser(request)
            
            setUserPassword(username, password)

            return response.user().username()
        } catch (e: Exception) {
            when (e) {
                is UsernameExistsException -> throw IllegalArgumentException("Username already exists")
                is InvalidParameterException -> throw IllegalArgumentException(e.message)
                else -> throw RuntimeException(e.message)
            }
        }
    }

    private fun setUserPassword(username: String, password: String) {
        val request = AdminSetUserPasswordRequest.builder()
            .userPoolId(userPoolId)
            .username(username)
            .password(password)
            .permanent(true)
            .build()

        cognitoClient.adminSetUserPassword(request)
    }

    override fun loadUserByUsername(username: String): UserDetails {
        try {
            val request = AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .build()

            val userResponse = cognitoClient.adminGetUser(request)
            
            val groups = getUserGroups(username)
            val authorities = groups.map { SimpleGrantedAuthority("ROLE_$it") }.toMutableList()
            
            authorities.add(SimpleGrantedAuthority("ROLE_USER"))
            
            return User(
                userResponse.username(),
                "",
                true,
                true,
                true,
                userResponse.enabled(),
                authorities
            )
        } catch (e: UserNotFoundException) {
            throw UsernameNotFoundException(e.message)
        } catch (e: Exception) {
            logger.error(e.message)
            throw UsernameNotFoundException(e.message)
        }
    }

    private fun getUserGroups(username: String): List<String> {
        val request = AdminListGroupsForUserRequest.builder()
            .userPoolId(userPoolId)
            .username(username)
            .build()

        val response = cognitoClient.adminListGroupsForUser(request)
        return response.groups().map { it.groupName() }
    }

    private fun getUserIdByUsername(username: String): String? {
        try {
            val request = AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .build()

            val response = cognitoClient.adminGetUser(request)
            
            return response.username()
        } catch (e: Exception) {
            logger.error(e.message)
            return null
        }
    }

    fun refreshToken(refreshToken: String): AuthenticationResultType? {
        try {
            val authParams = mapOf(
                "REFRESH_TOKEN" to refreshToken
            )

            val request = AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .authParameters(authParams)
                .build()

            val response = cognitoClient.adminInitiateAuth(request)
            return response.authenticationResult()
        } catch (e: Exception) {
            logger.error(e.message)
            return null
        }
    }
}
