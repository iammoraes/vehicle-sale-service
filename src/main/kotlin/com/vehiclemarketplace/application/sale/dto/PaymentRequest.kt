package com.vehiclemarketplace.application.sale.dto

import com.vehiclemarketplace.domain.model.sale.PaymentMethod
import java.math.BigDecimal

data class PaymentRequest(
    val method: PaymentMethod,
    val amount: BigDecimal
)
