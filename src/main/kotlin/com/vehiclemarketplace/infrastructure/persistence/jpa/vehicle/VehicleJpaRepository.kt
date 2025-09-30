package com.vehiclemarketplace.infrastructure.persistence.jpa.vehicle

import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import com.vehiclemarketplace.infrastructure.persistence.entities.vehicle.VehicleEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VehicleJpaRepository : JpaRepository<VehicleEntity, UUID> {

    @Query("SELECT v FROM VehicleEntity v WHERE v.deleted = false AND (:status IS NULL OR v.status = :status) ORDER BY v.price ASC")
    fun findAllWithFilters(
        @Param("status") status: VehicleStatus?,
        pageable: Pageable
    ): Page<VehicleEntity>

    override fun findById(id: UUID): Optional<VehicleEntity>

    @Query("SELECT COUNT(v) FROM VehicleEntity v WHERE v.status = :status")
    fun countByStatus(@Param("status") status: VehicleStatus): Long
}
