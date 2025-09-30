package com.vehiclemarketplace.domain.model.vehicle

import com.vehiclemarketplace.domain.model.BaseModel
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Year
import java.util.*

internal typealias VehicleId = UUID

data class Vehicle(
    override val id: VehicleId? = null,
    val chassis: String,
    val brand: String,
    val model: String,
    val manufacturingYear: Year,
    val modelYear: Year,
    val color: String,
    val mileage: Int,
    val price: BigDecimal,
    val description: String,
    val status: VehicleStatus = VehicleStatus.AVAILABLE,
    override val createdAt: LocalDateTime = LocalDateTime.now(),
    override val updatedAt: LocalDateTime = LocalDateTime.now(),
) : BaseModel<VehicleId>(id, createdAt, updatedAt) {
    
    fun isAvailable() = status == VehicleStatus.AVAILABLE
    
    fun reserve() = copy(status = VehicleStatus.RESERVED)
    
    fun sell() = copy(status = VehicleStatus.SOLD)
    
    fun makeAvailable() = copy(status = VehicleStatus.AVAILABLE)
}

enum class VehicleStatus {
    AVAILABLE,
    RESERVED,
    SOLD
}
