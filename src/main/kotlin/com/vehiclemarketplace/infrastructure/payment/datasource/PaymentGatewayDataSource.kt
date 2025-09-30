package com.vehiclemarketplace.infrastructure.payment.datasource

import com.vehiclemarketplace.domain.model.buyer.Buyer
import java.math.BigDecimal

interface PaymentGatewayDataSource {
    fun createPayment(
        amount: BigDecimal,
        method: String,
        buyer: Buyer,
        callbackUrl: String?
    ): Map<String, Any>
    fun getPayment(paymentId: String): Map<String, Any>
    fun cancelPayment(paymentId: String): Map<String, Any>
}
