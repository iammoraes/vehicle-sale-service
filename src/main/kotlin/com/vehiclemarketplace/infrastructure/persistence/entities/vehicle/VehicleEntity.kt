package com.vehiclemarketplace.infrastructure.persistence.entities.vehicle

import com.vehiclemarketplace.domain.model.vehicle.Vehicle
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import com.vehiclemarketplace.infrastructure.persistence.entities.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Year

@Entity
@Table(name = "vehicles")
class VehicleEntity(
    @Column(nullable = false)
    var brand: String,

    @Column(nullable = false)
    var chassis: String,

    @Column(nullable = false)
    var model: String,

    @Column(name = "manufacturing_year", nullable = false)
    var manufacturingYear: Int,

    @Column(name = "model_year", nullable = false)
    var modelYear: Int,

    @Column(nullable = false)
    var color: String,

    @Column(nullable = false)
    var mileage: Int,

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal,

    @Column(columnDefinition = "TEXT")
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VehicleStatus = VehicleStatus.AVAILABLE,
) : BaseEntity() {

    fun toDomain(): Vehicle {
        return Vehicle(
            id = id,
            brand = brand,
            chassis = chassis,
            model = model,
            manufacturingYear = Year.of(manufacturingYear),
            modelYear = Year.of(modelYear),
            color = color,
            mileage = mileage,
            price = price,
            description = description,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(vehicle: Vehicle): VehicleEntity {
            return VehicleEntity(
                brand = vehicle.brand,
                chassis = vehicle.chassis,
                model = vehicle.model,
                manufacturingYear = vehicle.manufacturingYear.value,
                modelYear = vehicle.modelYear.value,
                color = vehicle.color,
                mileage = vehicle.mileage,
                price = vehicle.price,
                description = vehicle.description,
                status = vehicle.status
            ).apply {
                id = vehicle.id
                createdAt = vehicle.createdAt
                updatedAt = vehicle.updatedAt
            }
        }
    }
}
