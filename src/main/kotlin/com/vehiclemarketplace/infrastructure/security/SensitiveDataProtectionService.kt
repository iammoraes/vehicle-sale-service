package com.vehiclemarketplace.infrastructure.security

import com.vehiclemarketplace.infrastructure.aws.AwsDataProtectionService
import com.vehiclemarketplace.infrastructure.persistence.entities.buyer.BuyerEntity
import com.vehiclemarketplace.infrastructure.persistence.entities.buyer.DocumentEntity
import com.vehiclemarketplace.infrastructure.persistence.entities.sale.PaymentEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SensitiveDataProtectionService(
    private val awsDataProtectionService: AwsDataProtectionService,
    @Value("\${app.security.encryption.enabled:true}") private val encryptionEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun protectBuyerData(buyer: BuyerEntity) {
        if (!encryptionEnabled) return
        
        try {
            buyer.email = awsDataProtectionService.encrypt(buyer.email)
            buyer.phone = awsDataProtectionService.encrypt(buyer.phone)
            
            buyer.documents.forEach { protectDocumentData(it) }
        } catch (e: Exception) {
            logger.error("Error encrypting buyer data: ${e.message}")
            throw SecurityException("Error encrypting sensitive data", e)
        }
    }
    
    fun revealBuyerData(buyer: BuyerEntity) {
        if (!encryptionEnabled) return
        
        try {
            buyer.email = awsDataProtectionService.decrypt(buyer.email)
            buyer.phone = awsDataProtectionService.decrypt(buyer.phone)
            
            buyer.documents.forEach { revealDocumentData(it) }
        } catch (e: Exception) {
            logger.error("Error decrypting buyer data: ${e.message}")
            throw SecurityException("Error decrypting sensitive data", e)
        }
    }
    
    fun protectDocumentData(document: DocumentEntity) {
        if (!encryptionEnabled) return
        
        try {
            document.number = awsDataProtectionService.encrypt(document.number)
        } catch (e: Exception) {
            logger.error("Error encrypting document: ${e.message}")
            throw SecurityException("Error encrypting document", e)
        }
    }
    
    fun revealDocumentData(document: DocumentEntity) {
        if (!encryptionEnabled) return
        
        try {
            document.number = awsDataProtectionService.decrypt(document.number)
        } catch (e: Exception) {
            logger.error("Error decrypting document: ${e.message}")
            throw SecurityException("Error decrypting document", e)
        }
    }
    
    fun protectPaymentDetails(payment: PaymentEntity, originalDetails: String): String {
        if (!encryptionEnabled) return originalDetails
        
        try {
            return awsDataProtectionService.encrypt(originalDetails)
        } catch (e: Exception) {
            logger.error("Error encrypting payment details: ${e.message}")
            throw SecurityException("Error encrypting payment data", e)
        }
    }
    
    fun revealPaymentDetails(encryptedDetails: String): String {
        if (!encryptionEnabled) return encryptedDetails
        
        try {
            return awsDataProtectionService.decrypt(encryptedDetails)
        } catch (e: Exception) {
            logger.error("Error decrypting payment details: ${e.message}")
            throw SecurityException("Error decrypting payment data", e)
        }
    }
    
    fun isLikelyEncrypted(text: String): Boolean {
        return text.matches(Regex("^[A-Za-z0-9+/=]{24,}$"))
    }
}
