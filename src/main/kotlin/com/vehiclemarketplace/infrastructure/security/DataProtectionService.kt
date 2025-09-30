package com.vehiclemarketplace.infrastructure.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

@Service
class DataProtectionService(
    @Value("\${app.encryption.key:}") private val encryptionKey: String
) {
    private val algorithm = "AES"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmIvLength = 12
    private val gcmTagLength = 16

    private val secretKey: SecretKey by lazy {
        if (encryptionKey.isNotEmpty()) {
            SecretKeySpec(Base64.getDecoder().decode(encryptionKey), algorithm)
        } else {
            generateKey()
        }
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(algorithm)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts sensitive data for storage (LGPD compliance)
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(transformation)
        val iv = ByteArray(gcmIvLength)
        SecureRandom().nextBytes(iv)
        
        val parameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        
        val encryptedText = cipher.doFinal(plainText.toByteArray())
        val encryptedWithIv = ByteArray(gcmIvLength + encryptedText.size)
        
        System.arraycopy(iv, 0, encryptedWithIv, 0, gcmIvLength)
        System.arraycopy(encryptedText, 0, encryptedWithIv, gcmIvLength, encryptedText.size)
        
        return Base64.getEncoder().encodeToString(encryptedWithIv)
    }

    /**
     * Decrypts sensitive data from storage (LGPD compliance)
     */
    fun decrypt(encryptedText: String): String {
        val encryptedWithIv = Base64.getDecoder().decode(encryptedText)
        
        val iv = ByteArray(gcmIvLength)
        System.arraycopy(encryptedWithIv, 0, iv, 0, gcmIvLength)
        
        val encrypted = ByteArray(encryptedWithIv.size - gcmIvLength)
        System.arraycopy(encryptedWithIv, gcmIvLength, encrypted, 0, encrypted.size)
        
        val cipher = Cipher.getInstance(transformation)
        val parameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        val decryptedText = cipher.doFinal(encrypted)
        return String(decryptedText)
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
        return "ANON_${abs(hash)}"
    }
}
