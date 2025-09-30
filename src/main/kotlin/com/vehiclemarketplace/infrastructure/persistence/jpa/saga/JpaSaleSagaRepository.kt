package com.vehiclemarketplace.infrastructure.persistence.jpa.saga

import com.vehiclemarketplace.domain.model.saga.SagaStatus
import com.vehiclemarketplace.infrastructure.persistence.entities.saga.SaleSagaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaSaleSagaRepository : JpaRepository<SaleSagaEntity, UUID> {
    fun findByStatus(status: SagaStatus): List<SaleSagaEntity>
    @Query("SELECT s FROM SaleSagaEntity s WHERE s.status = 'STARTED' OR s.status = 'IN_PROGRESS'")
    fun findActiveSagas(): List<SaleSagaEntity>
    fun findByStatusAndLastErrorIsNotNull(status: SagaStatus): List<SaleSagaEntity>
    fun findByVehicleId(vehicleId: UUID): List<SaleSagaEntity>
    fun findByBuyerId(buyerId: UUID): List<SaleSagaEntity>
    fun findBySaleId(saleId: UUID): SaleSagaEntity?
}
