package com.vehiclemarketplace.domain.service.payment

import com.vehiclemarketplace.application.payment.dto.NotificationRequest
import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse
import com.vehiclemarketplace.domain.model.sale.Payment

interface PaymentService {
    fun createPayment(payment: Payment, buyer: Buyer): PaymentGatewayResponse
    fun processPayment(payment: Payment): Payment
    fun cancelPayment(paymentId: String): PaymentGatewayResponse
    suspend fun processPaymentNotification(notification: NotificationRequest): Map<String, String>
}