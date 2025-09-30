package com.vehiclemarketplace.infrastructure.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import java.util.*
import javax.annotation.PostConstruct

@Service
class AwsDataProtectionService(
    internal val kmsClient: KmsClient,
    @Value("\${aws.kms.secret-name:kms-config}") private val kmsSecretName: String,
    private val secretsManagerService: AwsSecretsManagerService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    private lateinit var keyId: String
    
    @PostConstruct
    fun init() {
        keyId = secretsManagerService.getSecretJsonValue(kmsSecretName, "key_id")
            ?: throw IllegalStateException("Failed to load KMS key from Secrets Manager")
    }

    fun encrypt(plainText: String): String {
        return try {
            val encryptRequest = EncryptRequest.builder()
                .keyId(keyId)
                .plaintext(SdkBytes.fromUtf8String(plainText))
                .build()
            
            val encryptResponse = kmsClient.encrypt(encryptRequest)
            Base64.getEncoder().encodeToString(encryptResponse.ciphertextBlob().asByteArray())
        } catch (e: Exception) {
            logger.error(e.message)
            throw SecurityException(e.message, e)
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val encryptedBytes = Base64.getDecoder().decode(encryptedText)
            
            val decryptRequest = DecryptRequest.builder()
                .keyId(keyId)
                .ciphertextBlob(SdkBytes.fromByteArray(encryptedBytes))
                .build()
            
            val decryptResponse = kmsClient.decrypt(decryptRequest)
            decryptResponse.plaintext().asUtf8String()
        } catch (e: Exception) {
            logger.error(e.message)
            throw SecurityException(e.message, e)
        }
    }

    fun anonymizeCpf(cpf: String): String {
        return "***.***.***-**"
    }

    fun anonymizeEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***@***.***"
        
        val localPart = parts[0]
        val domain = parts[1]
        
        val anonymizedLocal = if (localPart.length <= 2) {
            "***"
        } else {
            localPart.take(2) + "*".repeat(localPart.length - 2)
        }
        
        return "$anonymizedLocal@$domain"
    }

    fun anonymizePhone(phone: String): String {
        return phone.replace(Regex("\\d"), "*")
    }

    fun anonymizeName(name: String): String {
        val parts = name.split(" ")
        return parts.mapIndexed { index, part ->
            when {
                index == 0 -> part.take(1) + "*".repeat(maxOf(0, part.length - 1))
                index == parts.lastIndex && parts.size > 1 -> part.take(1) + "*".repeat(maxOf(0, part.length - 1))
                else -> "*".repeat(part.length)
            }
        }.joinToString(" ")
    }

    fun maskCpf(cpf: String): String {
        return if (cpf.length >= 11) {
            "${cpf.take(3)}.***.***.${cpf.takeLast(2)}"
        } else {
            "***.***.***-**"
        }
    }

    fun maskEmail(email: String): String {
        val atIndex = email.indexOf("@")
        return if (atIndex > 2) {
            "${email.take(2)}***${email.substring(atIndex)}"
        } else {
            "***@***.***"
        }
    }

    fun maskCreditCard(cardNumber: String): String {
        return if (cardNumber.length >= 16) {
            "**** **** **** ${cardNumber.takeLast(4)}"
        } else {
            "**** **** **** ****"
        }
    }

    fun generatePseudonym(originalId: String): String {
        val hash = originalId.hashCode()
        return "ANON_${Math.abs(hash)}"
    }
}
