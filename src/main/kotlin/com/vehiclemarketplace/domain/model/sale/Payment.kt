package com.vehiclemarketplace.domain.model.sale

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class Payment(
    val id: UUID = UUID.randomUUID(),
    val amount: BigDecimal,
    val currency: String = "BRL",
    val method: PaymentMethod,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val paymentDate: LocalDateTime? = null,
    val dueDate: LocalDateTime? = null,
    val paymentDetails: PaymentDetails?,
    val transactionId: String? = null
) {
    fun process() = copy(status = PaymentStatus.PROCESSING)
    
    fun approve(transactionId: String) = copy(
        status = PaymentStatus.APPROVED,
        paymentDate = LocalDateTime.now(),
        transactionId = transactionId
    )
    
    fun decline(reason: String) = copy(
        status = PaymentStatus.DECLINED,
        paymentDate = LocalDateTime.now()
    )

    fun expire() = copy(
        status = PaymentStatus.EXPIRED,
        paymentDate = LocalDateTime.now()
    )

    fun cancel() = copy(
        status = PaymentStatus.CANCELLED,
        paymentDate = LocalDateTime.now()
    )
}
