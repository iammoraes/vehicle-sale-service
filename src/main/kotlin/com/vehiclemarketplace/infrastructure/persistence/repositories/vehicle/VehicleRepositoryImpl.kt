package com.vehiclemarketplace.infrastructure.persistence.repositories.vehicle

import com.vehiclemarketplace.domain.model.vehicle.Vehicle
import com.vehiclemarketplace.domain.model.vehicle.VehicleId
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import com.vehiclemarketplace.domain.repositories.vehicle.VehicleRepository
import com.vehiclemarketplace.infrastructure.exception.VehicleNotFoundException
import com.vehiclemarketplace.infrastructure.persistence.entities.vehicle.VehicleEntity
import com.vehiclemarketplace.infrastructure.persistence.jpa.vehicle.VehicleJpaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class VehicleRepositoryImpl(
    private val jpaRepository: VehicleJpaRepository
) : VehicleRepository {

    override suspend fun findById(id: VehicleId): Vehicle = withContext(Dispatchers.IO) {
        jpaRepository.findById(id).orElseThrow { throw VehicleNotFoundException("Vehicle not found: $id") }.toDomain()
    }

    override suspend fun findAll(
        status: VehicleStatus?,
        page: Int,
        size: Int
    ): List<Vehicle> = withContext(Dispatchers.IO) {
        val pageable = PageRequest.of(page, size, Sort.by("price").ascending())

        jpaRepository.findAllWithFilters(
            status = status,
            pageable = pageable
        ).content.map { it.toDomain() }
    }

    override suspend fun save(vehicle: Vehicle): Vehicle = withContext(Dispatchers.IO) {
        val entity = VehicleEntity.Companion.fromDomain(vehicle)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun update(vehicle: Vehicle): Vehicle = withContext(Dispatchers.IO) {
        try {
            val savedEntity = jpaRepository.save(VehicleEntity.fromDomain(vehicle))
            return@withContext savedEntity.toDomain()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to update vehicle: ${e.message}", e)
        }
    }
    override suspend fun countByStatus(status: VehicleStatus): Long = withContext(Dispatchers.IO) {
        jpaRepository.countByStatus(status)
    }
}