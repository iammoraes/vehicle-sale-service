package com.vehiclemarketplace.application.sale.dto

import java.util.*

data class SaleRequest(
    val vehicleId: UUID,
    val buyerId: UUID,
    val payment: PaymentRequest
)
