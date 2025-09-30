package com.vehiclemarketplace.domain.repositories.vehicle

import com.vehiclemarketplace.domain.model.vehicle.Vehicle
import com.vehiclemarketplace.domain.model.vehicle.VehicleId
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import java.util.*

interface VehicleRepository {
    suspend fun findById(id: VehicleId): Vehicle
    suspend fun findAll(
        status: VehicleStatus? = null,
        page: Int = 0,
        size: Int = 20
    ): List<Vehicle>
    
    suspend fun save(vehicle: Vehicle): Vehicle
    suspend fun update(vehicle: Vehicle): Vehicle

    suspend fun countByStatus(status: VehicleStatus): Long
}
