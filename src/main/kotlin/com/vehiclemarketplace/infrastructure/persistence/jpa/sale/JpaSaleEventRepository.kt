package com.vehiclemarketplace.infrastructure.persistence.jpa.sale

import com.vehiclemarketplace.infrastructure.persistence.entities.sale.SaleEventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaSaleEventRepository : JpaRepository<SaleEventEntity, UUID> {
    fun findBySaleIdOrderByTimestampAsc(saleId: UUID): List<SaleEventEntity>
}
