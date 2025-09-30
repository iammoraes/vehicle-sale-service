package com.vehiclemarketplace.domain.model.sale

import com.vehiclemarketplace.domain.model.BaseModel
import com.vehiclemarketplace.domain.model.buyer.BuyerId
import com.vehiclemarketplace.domain.model.vehicle.VehicleId
import java.time.LocalDateTime
import java.util.*

typealias SaleId = UUID

data class Sale(
    override val id: SaleId,
    val vehicleId: VehicleId,
    val buyerId: BuyerId,
    val status: SaleStatus,
    val payment: Payment,
    val notes: String? = null,
    val events: MutableList<SaleEvent> = mutableListOf(),
    override val createdAt: LocalDateTime = LocalDateTime.now(),
    override val updatedAt: LocalDateTime = LocalDateTime.now(),
) : BaseModel<SaleId>(id, createdAt, updatedAt) {

    fun addEvent(event: SaleEvent) {
        events.add(event)
    }

    fun updateStatus(newStatus: SaleStatus) = copy(
        status = newStatus,
        updatedAt = LocalDateTime.now()
    )

    fun cancel(reason: String) = copy(
        status = SaleStatus.CANCELLED,
        notes = reason,
        updatedAt = LocalDateTime.now()
    )
}