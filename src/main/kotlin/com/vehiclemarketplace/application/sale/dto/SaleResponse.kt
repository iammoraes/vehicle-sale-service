package com.vehiclemarketplace.application.sale.dto

import com.vehiclemarketplace.application.buyer.dto.BuyerDto
import com.vehiclemarketplace.application.vehicle.dto.VehicleDto
import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.sale.Sale
import com.vehiclemarketplace.domain.model.vehicle.Vehicle
import java.util.*

data class SaleResponse(
    val id: UUID,
    val vehicleId: UUID,
    val buyerId: UUID,
    val status: String,
    val payment: PaymentResponse,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    val vehicle: VehicleDto? = null,
    val buyer: BuyerDto? = null
) {
    companion object {
        fun fromDomain(sale: Sale, vehicle: Vehicle? = null, buyer: Buyer? = null) = SaleResponse(
            id = sale.id,
            vehicleId = sale.vehicleId,
            buyerId = sale.buyerId,
            status = sale.status.name,
            payment = PaymentResponse.fromDomain(sale.payment),
            createdAt = sale.createdAt.toString(),
            updatedAt = sale.updatedAt.toString(),
            notes = sale.notes,
            vehicle = vehicle?.let { VehicleDto.fromDomain(it) },
            buyer = buyer?.let { BuyerDto.fromDomain(it) }
        )
    }
}
