package com.vehiclemarketplace.domain.util

import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayStatus
import com.vehiclemarketplace.domain.model.sale.BoletoDetails
import com.vehiclemarketplace.domain.model.sale.Payment
import com.vehiclemarketplace.domain.model.sale.PaymentMethod
import com.vehiclemarketplace.domain.model.sale.PaymentStatus
import com.vehiclemarketplace.domain.model.sale.PixDetails
import java.time.LocalDateTime

object PaymentUtil {

    fun mapGatewayStatusToPaymentStatus(status: PaymentGatewayStatus): PaymentStatus {
        return when (status) {
            PaymentGatewayStatus.PENDING -> PaymentStatus.PENDING
            PaymentGatewayStatus.PROCESSING -> PaymentStatus.PROCESSING
            PaymentGatewayStatus.APPROVED -> PaymentStatus.APPROVED
            PaymentGatewayStatus.DECLINED -> PaymentStatus.DECLINED
            PaymentGatewayStatus.CANCELLED -> PaymentStatus.CANCELLED
            PaymentGatewayStatus.EXPIRED -> PaymentStatus.EXPIRED
            PaymentGatewayStatus.ERROR -> PaymentStatus.DECLINED
        }
    }


    fun updatePaymentWithGatewayResponse(payment: Payment, response: PaymentGatewayResponse): Payment {
        val paymentDetails = when (payment.method) {
            PaymentMethod.PIX -> PixDetails(
                pixKey = response.qrCode ?: "",
                expirationDate = response.expirationDate ?: LocalDateTime.now().plusDays(1)
            )
            PaymentMethod.BOLETO -> BoletoDetails(
                barcode = response.barcode ?: "",
                dueDate = response.expirationDate,
                pdfUrl = response.pdfUrl ?: ""
            )
        }

        return payment.copy(
            status = mapGatewayStatusToPaymentStatus(response.status),
            paymentDate = if (response.status == PaymentGatewayStatus.APPROVED) response.processingDate else null,
            paymentDetails = paymentDetails,
            transactionId = response.transactionId
        )
    }
}