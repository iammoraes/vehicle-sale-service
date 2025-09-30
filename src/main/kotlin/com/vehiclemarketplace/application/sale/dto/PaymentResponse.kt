package com.vehiclemarketplace.application.sale.dto

import com.vehiclemarketplace.domain.model.sale.Payment
import java.math.BigDecimal
import java.util.*

data class PaymentResponse(
    val id: UUID,
    val amount: BigDecimal,
    val currency: String,
    val method: String,
    val status: String,
    val paymentDate: String? = null,
    val dueDate: String? = null,
    val transactionId: String? = null
) {
    companion object {
        fun fromDomain(payment: Payment) = PaymentResponse(
            id = payment.id,
            amount = payment.amount,
            currency = payment.currency,
            method = payment.method.name,
            status = payment.status.name,
            paymentDate = payment.paymentDate?.toString(),
            dueDate = payment.dueDate?.toString(),
            transactionId = payment.transactionId
        )
    }
}
