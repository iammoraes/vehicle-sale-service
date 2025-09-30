package com.vehiclemarketplace.application.mapper

import com.vehiclemarketplace.application.sale.dto.PaymentRequest
import com.vehiclemarketplace.domain.model.sale.Payment
import com.vehiclemarketplace.domain.model.sale.PaymentDetails
import com.vehiclemarketplace.domain.model.sale.PaymentMethod
import com.vehiclemarketplace.domain.model.sale.PaymentStatus
import java.time.LocalDateTime
import java.util.*

object PaymentMapper {

    fun toPayment(
        request: PaymentRequest, 
        paymentDetails: PaymentDetails? = null
    ): Payment {
        return Payment(
            id = UUID.randomUUID(),
            amount = request.amount,
            currency = "BRL",
            method = request.method,
            status = PaymentStatus.PENDING,
            paymentDetails = paymentDetails,
            paymentDate = null,
            dueDate = calculateDueDate(request.method),
            transactionId = null
        )
    }

    private fun calculateDueDate(method: PaymentMethod): LocalDateTime? {
        return when(method) {
            PaymentMethod.PIX -> LocalDateTime.now().plusHours(24)
            PaymentMethod.BOLETO -> LocalDateTime.now().plusDays(3)
        }
    }
}
