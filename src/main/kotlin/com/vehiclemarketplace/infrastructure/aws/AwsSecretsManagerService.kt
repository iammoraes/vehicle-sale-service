package com.vehiclemarketplace.infrastructure.aws

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException

@Service
class AwsSecretsManagerService(
    private val secretsManagerClient: SecretsManagerClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secretCache = mutableMapOf<String, CachedSecret>()
    private val cacheTtlMs = 900000L

    fun getSecretString(secretName: String): String? {
        try {
            val cachedSecret = secretCache[secretName]
            if (cachedSecret != null && !isCacheExpired(cachedSecret)) {
                return cachedSecret.value
            }

            val valueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build()

            val valueResponse = secretsManagerClient.getSecretValue(valueRequest)
            val secretValue = valueResponse.secretString()

            secretCache[secretName] = CachedSecret(secretValue, System.currentTimeMillis())

            return secretValue
        } catch (e: ResourceNotFoundException) {
            logger.warn(e.message)
            return null
        } catch (e: Exception) {
            logger.error(e.message)
            throw SecurityException(e.message, e)
        }
    }

    fun getSecretJsonValue(secretName: String, key: String): String? {
        val secretJson = getSecretString(secretName) ?: return null

        return try {
            val jsonNode = objectMapper.readTree(secretJson)
            jsonNode.get(key)?.asText()
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }

    fun getSecretBinary(secretName: String): ByteArray? {
        try {
            val valueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build()

            val valueResponse = secretsManagerClient.getSecretValue(valueRequest)
            val secretBinary = valueResponse.secretBinary()

            return secretBinary?.asByteArray()
        } catch (e: Exception) {
            logger.error(e.message)
            return null
        }
    }

    fun getDatabaseCredentials(secretName: String): DatabaseCredentials? {
        val secretJson = getSecretString(secretName) ?: return null

        return try {
            val jsonNode = objectMapper.readTree(secretJson)
            DatabaseCredentials(
                username = jsonNode.get("username").asText(),
                password = jsonNode.get("password").asText(),
                engine = jsonNode.get("engine")?.asText(),
                host = jsonNode.get("host")?.asText(),
                port = jsonNode.get("port")?.asInt(),
                dbname = jsonNode.get("dbname")?.asText()
            )
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }

    fun getApiCredentials(secretName: String): ApiCredentials? {
        val secretJson = getSecretString(secretName) ?: return null

        return try {
            val jsonNode = objectMapper.readTree(secretJson)
            ApiCredentials(
                apiKey = jsonNode.get("apiKey").asText(),
                apiSecret = jsonNode.get("apiSecret")?.asText(),
                endpoint = jsonNode.get("endpoint")?.asText()
            )
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }

    private fun isCacheExpired(cachedSecret: CachedSecret): Boolean {
        return System.currentTimeMillis() - cachedSecret.timestamp > cacheTtlMs
    }

    fun clearCache() {
        secretCache.clear()
    }

    private data class CachedSecret(val value: String, val timestamp: Long)

    data class DatabaseCredentials(
        val username: String,
        val password: String,
        val engine: String? = null,
        val host: String? = null,
        val port: Int? = null,
        val dbname: String? = null
    )

    data class ApiCredentials(
        val apiKey: String,
        val apiSecret: String? = null,
        val endpoint: String? = null
    )
}
