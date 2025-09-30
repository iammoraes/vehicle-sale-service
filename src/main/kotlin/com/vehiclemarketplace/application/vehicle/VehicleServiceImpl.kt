package com.vehiclemarketplace.application.vehicle

import com.vehiclemarketplace.application.vehicle.dto.UpdateVehicleRequest
import com.vehiclemarketplace.application.vehicle.dto.VehicleDto
import com.vehiclemarketplace.domain.model.vehicle.Vehicle
import com.vehiclemarketplace.domain.model.vehicle.VehicleId
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import com.vehiclemarketplace.domain.repositories.vehicle.VehicleRepository
import com.vehiclemarketplace.domain.service.vehicle.VehicleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Year

@Service
@Transactional
class VehicleServiceImpl(
    private val vehicleRepository: VehicleRepository
) : VehicleService {

    override suspend fun listVehicles(
        status: VehicleStatus?,
        pageable: Pageable
    ): Page<VehicleDto> = withContext(Dispatchers.IO) {
        val vehicles = vehicleRepository.findAll(
            status = status,
            page = pageable.pageNumber,
            size = pageable.pageSize
        )

        val vehicleDtos = vehicles.map { VehicleDto.fromDomain(it) }

        PageImpl(
            vehicleDtos,
            pageable,
            vehicleRepository.countByStatus(status ?: VehicleStatus.AVAILABLE)
        )
    }

    override suspend fun createVehicle(request: VehicleDto): VehicleDto = withContext(Dispatchers.IO) {
        val vehicle = Vehicle(
            brand = request.brand,
            model = request.model,
            manufacturingYear = Year.of(request.manufacturingYear),
            modelYear = Year.of(request.modelYear),
            color = request.color,
            mileage = request.mileage,
            price = request.price,
            description = request.description,
            chassis = request.chassis,
        )

        val savedVehicle = vehicleRepository.save(vehicle)
        VehicleDto.fromDomain(savedVehicle)
    }

    override suspend fun updateVehicle(id: VehicleId, request: UpdateVehicleRequest): VehicleDto? =
        withContext(Dispatchers.IO) {
            try {
                val existingVehicle = vehicleRepository.findById(id)
                val updatedVehicle = existingVehicle.copy(
                    brand = request.brand ?: existingVehicle.brand,
                    model = request.model ?: existingVehicle.model,
                    manufacturingYear = request.manufacturingYear?.let { Year.of(it) }
                        ?: existingVehicle.manufacturingYear,
                    modelYear = request.modelYear?.let { Year.of(it) } ?: existingVehicle.modelYear,
                    color = request.color ?: existingVehicle.color,
                    mileage = request.mileage ?: existingVehicle.mileage,
                    price = request.price ?: existingVehicle.price,
                    description = request.description ?: existingVehicle.description,
                    status = request.status?.let { VehicleStatus.valueOf(it) } ?: existingVehicle.status,
                    updatedAt = LocalDateTime.now()
                )

                val savedVehicle = vehicleRepository.update(updatedVehicle)
                return@withContext VehicleDto.fromDomain(savedVehicle)
            } catch (e: Exception) {
                throw RuntimeException("Failed to update vehicle: ${e.message}", e)
            }
        }
}
