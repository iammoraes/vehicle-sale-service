package com.vehiclemarketplace.application.vehicle.dto

import com.vehiclemarketplace.domain.model.vehicle.Vehicle
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class VehicleDto(
    val id: UUID?,
    val chassis: String,
    val brand: String,
    val model: String,
    val manufacturingYear: Int,
    val modelYear: Int,
    val color: String,
    val mileage: Int,
    val price: BigDecimal,
    val description: String,
    val status: String,
    val createdAt: String? = LocalDateTime.now().toString(),
    val updatedAt: String? = LocalDateTime.now().toString()
) {
    companion object {
        fun fromDomain(vehicle: Vehicle) = VehicleDto(
            id = vehicle.id,
            chassis = vehicle.chassis,
            brand = vehicle.brand,
            model = vehicle.model,
            manufacturingYear = vehicle.manufacturingYear.value,
            modelYear = vehicle.modelYear.value,
            color = vehicle.color,
            mileage = vehicle.mileage,
            price = vehicle.price,
            description = vehicle.description,
            status = vehicle.status.name,
            createdAt = vehicle.createdAt.toString(),
            updatedAt = vehicle.updatedAt.toString(),
        )
    }
}

data class UpdateVehicleRequest(
    val brand: String? = null,
    val model: String? = null,
    val manufacturingYear: Int? = null,
    val modelYear: Int? = null,
    val color: String? = null,
    val mileage: Int? = null,
    val price: BigDecimal? = null,
    val description: String? = null,
    val status: String? = null
)
