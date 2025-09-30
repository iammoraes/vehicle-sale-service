package com.vehiclemarketplace.domain.repositories.payment

import com.vehiclemarketplace.domain.model.payment.PaymentGatewayRequest
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse

interface PaymentGatewayRepository {
    fun createPayment(request: PaymentGatewayRequest): PaymentGatewayResponse
    fun getPayment(paymentId: String): PaymentGatewayResponse
    fun cancelPayment(paymentId: String): PaymentGatewayResponse
}
