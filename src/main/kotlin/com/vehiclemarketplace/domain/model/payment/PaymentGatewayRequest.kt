package com.vehiclemarketplace.domain.model.payment

import com.vehiclemarketplace.domain.model.buyer.Buyer
import java.math.BigDecimal

data class PaymentGatewayRequest(
    val amount: BigDecimal,
    val paymentMethod: PaymentGatewayMethod,
    val buyer: Buyer,
    val callbackUrl: String? = null,
    val metadata: Map<String, Any> = mapOf()
)

enum class PaymentGatewayMethod {
    PIX,
    BOLETO;
    
    fun toExternalCode(gateway: String): String {
        return when (gateway) {
            "MERCADO_PAGO" -> when (this) {
                PIX -> "pix"
                BOLETO -> "boleto"
            }
            else -> this.name.lowercase()
        }
    }
}
