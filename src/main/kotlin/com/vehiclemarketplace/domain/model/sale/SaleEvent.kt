package com.vehiclemarketplace.domain.model.sale

import java.time.LocalDateTime
import java.util.*

data class SaleEvent(
    val id: UUID = UUID.randomUUID(),
    val saleId: SaleId,
    val eventType: SaleEventType,
    val payload: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
