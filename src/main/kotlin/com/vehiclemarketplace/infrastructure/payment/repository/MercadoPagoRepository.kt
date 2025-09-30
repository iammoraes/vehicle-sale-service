package com.vehiclemarketplace.infrastructure.payment.repository

import com.vehiclemarketplace.domain.exception.PaymentCreationException
import com.vehiclemarketplace.domain.exception.PaymentGatewayException
import com.vehiclemarketplace.domain.exception.PaymentNotFoundException
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayRequest
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayStatus
import com.vehiclemarketplace.domain.repositories.payment.PaymentGatewayRepository
import com.vehiclemarketplace.infrastructure.payment.datasource.PaymentGatewayDataSource
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Repository
class MercadoPagoRepository(
    private val dataSource: PaymentGatewayDataSource
) : PaymentGatewayRepository {
    private val GATEWAY_NAME = "MERCADO_PAGO"

    override fun createPayment(request: PaymentGatewayRequest): PaymentGatewayResponse {
        try {

            val methodCode = request.paymentMethod.toExternalCode(GATEWAY_NAME)
            
            val result = dataSource.createPayment(
                amount = request.amount,
                method = methodCode,
                buyer = request.buyer,
                callbackUrl = request.callbackUrl
            )
            
            return mapToPaymentGatewayResponse(result)
        } catch (ex: Exception) {
            throw PaymentCreationException("Failed to create payment through Mercado Pago", ex)
        }
    }

    override fun getPayment(paymentId: String): PaymentGatewayResponse {
        try {
            val result = dataSource.getPayment(paymentId)
            
            return mapToPaymentGatewayResponse(result)
        } catch (ex: Exception) {
            when {
                ex.message?.contains("not found", ignoreCase = true) == true ->
                    throw PaymentNotFoundException(paymentId, ex)
                else -> 
                    throw PaymentGatewayException(GATEWAY_NAME, "Error retrieving payment: ${ex.message}", ex)
            }
        }
    }

    override fun cancelPayment(paymentId: String): PaymentGatewayResponse {
        try {
            val result = dataSource.cancelPayment(paymentId)
            
            return mapToPaymentGatewayResponse(result)
        } catch (ex: Exception) {
            when {
                ex.message?.contains("not found", ignoreCase = true) == true -> 
                    throw PaymentNotFoundException(paymentId, ex)
                else -> 
                    throw PaymentGatewayException(GATEWAY_NAME, "Error cancelling payment: ${ex.message}", ex)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToPaymentGatewayResponse(gatewayResponse: Map<String, Any>): PaymentGatewayResponse {
        val id = gatewayResponse["id"]?.toString() ?: 
            throw PaymentGatewayException(GATEWAY_NAME, "Payment ID missing in gateway response")
        
        val status = gatewayResponse["status"]?.toString() ?: "UNKNOWN"
        val amount = gatewayResponse["transactionAmount"] as? BigDecimal ?: BigDecimal.ZERO
        
        val transactionId = id
        val processingDate = when (val dateCreated = gatewayResponse["dateCreated"]) {
            is OffsetDateTime -> dateCreated.toLocalDateTime()
            else -> LocalDateTime.now()
        }
        
        val qrCode = gatewayResponse["qrCode"]?.toString()
        val qrCodeBase64 = gatewayResponse["qrCodeBase64"]?.toString()
        val barcode = gatewayResponse["barcode"]?.toString()
        val pdfUrl = gatewayResponse["pdfUrl"]?.toString()
        
        val expirationDate = when (val dateExpiration = gatewayResponse["dateOfExpiration"]) {
            is OffsetDateTime -> dateExpiration.toLocalDateTime()
            else -> null
        }
        
        return PaymentGatewayResponse(
            id = id,
            amount = amount,
            status = PaymentGatewayStatus.fromString(status),
            transactionId = transactionId,
            processingDate = processingDate,
            paymentUrl = pdfUrl,
            qrCode = qrCode,
            qrCodeBase64 = qrCodeBase64,
            barcode = barcode,
            expirationDate = expirationDate,
            pdfUrl = pdfUrl
        )
    }
}
