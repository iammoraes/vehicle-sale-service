package com.vehiclemarketplace.domain.service.vehicle

import com.vehiclemarketplace.application.vehicle.dto.UpdateVehicleRequest
import com.vehiclemarketplace.application.vehicle.dto.VehicleDto
import com.vehiclemarketplace.domain.model.vehicle.VehicleId
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface VehicleService {
    suspend fun listVehicles(
        status: VehicleStatus?,
        pageable: Pageable
    ): Page<VehicleDto>


    suspend fun createVehicle(request: VehicleDto): VehicleDto

    suspend fun updateVehicle(id: VehicleId, request: UpdateVehicleRequest): VehicleDto?
}