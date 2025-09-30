package com.vehiclemarketplace.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey
import javax.annotation.PostConstruct

@Component
class JwtUtil(
    @Value("\${app.jwt.secret-name:jwt-secret}") private val jwtSecretName: String,
    @Value("\${app.jwt.expiration-ms}") private val jwtExpirationMs: Long,
    @Value("\${app.jwt.issuer}") private val issuer: String,
    private val secretsManagerService: AwsSecretsManagerService
) {
    private lateinit var key: SecretKey
    
    @PostConstruct
    fun init() {
        val secret = secretsManagerService.getSecretJsonValue(jwtSecretName, "secret")
            ?: throw IllegalStateException("Failed to load JWT key from Secrets Manager")
            
        this.key = Keys.hmacShaKeyFor(secret.toByteArray())
    }
    
    fun generateToken(userDetails: UserDetails, additionalClaims: Map<String, Any> = emptyMap()): String {
        val now = Date()
        val expirationDate = Date(now.time + jwtExpirationMs)
        
        return Jwts.builder()
            .setClaims(additionalClaims)
            .setSubject(userDetails.username)
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setExpiration(expirationDate)
            .signWith(key)
            .compact()
    }
    
    fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }
    
    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && !isTokenExpired(token)
    }
    
    fun extractAllClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
    
    private fun isTokenExpired(token: String): Boolean {
        return extractAllClaims(token).expiration.before(Date())
    }
}
