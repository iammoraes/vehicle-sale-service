package com.vehiclemarketplace.domain.model.sale

import java.time.LocalDateTime

data class PixDetails(
    val pixKey: String,
    val expirationDate: LocalDateTime
) : PaymentDetails
