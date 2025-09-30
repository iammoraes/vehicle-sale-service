package com.vehiclemarketplace.domain.model.payment

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentGatewayResponse(
    val id: String,
    val amount: BigDecimal,
    val status: PaymentGatewayStatus,
    val transactionId: String?,
    val processingDate: LocalDateTime,
    val paymentUrl: String?,
    val qrCode: String?,
    val qrCodeBase64: String?,
    val barcode: String?,
    val expirationDate: LocalDateTime?,
    val pdfUrl: String?
)

enum class PaymentGatewayStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    DECLINED,
    CANCELLED,
    EXPIRED,
    ERROR;

    companion object {
        fun fromString(status: String): PaymentGatewayStatus {
            return when (status.uppercase()) {
                "PENDING", "WAITING" -> PENDING
                "IN_PROCESS", "PROCESSING" -> PROCESSING
                "APPROVED", "COMPLETED", "SUCCESS" -> APPROVED
                "REJECTED", "DECLINED", "FAILURE" -> DECLINED
                "CANCELLED", "CANCELED" -> CANCELLED
                "EXPIRED" -> EXPIRED
                else -> ERROR
            }
        }
    }
}
