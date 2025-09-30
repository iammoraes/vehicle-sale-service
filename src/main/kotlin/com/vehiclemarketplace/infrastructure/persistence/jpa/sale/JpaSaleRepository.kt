package com.vehiclemarketplace.infrastructure.persistence.jpa.sale

import com.vehiclemarketplace.domain.model.sale.PaymentStatus
import com.vehiclemarketplace.domain.model.sale.SaleStatus
import com.vehiclemarketplace.infrastructure.persistence.entities.sale.SaleEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface JpaSaleRepository : JpaRepository<SaleEntity, UUID> {

    fun findByBuyerId(buyerId: UUID, pageable: Pageable): List<SaleEntity>

    fun findByVehicleId(vehicleId: UUID): SaleEntity?

    fun findByStatus(status: SaleStatus, pageable: Pageable): List<SaleEntity>

    @Query("SELECT s FROM SaleEntity s WHERE s.status = 'PENDING_PAYMENT' AND s.createdAt < :dateTime")
    fun findPendingSalesOlderThan(@Param("dateTime") dateTime: LocalDateTime): List<SaleEntity>

    @Query("""
        SELECT s FROM SaleEntity s
        JOIN s.payment p
        WHERE p.status = :status
    """)
    fun findSalesByPaymentStatus(@Param("status") status: PaymentStatus): List<SaleEntity>

    @Query("""
        SELECT COUNT(s) as totalSales, 
               COUNT(DISTINCT s.vehicleId) as totalVehiclesSold
        FROM SaleEntity s 
        WHERE s.createdAt BETWEEN :startDate AND :endDate 
        AND s.status = 'COMPLETED'
    """)
    fun getSalesSummaryBasic(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Map<String, Any>

    @Query("""
        SELECT s.status as status, COUNT(s) as count 
        FROM SaleEntity s 
        WHERE s.createdAt BETWEEN :startDate AND :endDate 
        GROUP BY s.status
    """)
    fun getSalesByStatus(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Map<String, Any>>

    @Query("""
        SELECT p.method as method, COUNT(p) as count 
        FROM SaleEntity s 
        JOIN s.payment p
        WHERE s.createdAt BETWEEN :startDate AND :endDate 
        AND s.status = 'COMPLETED'
        GROUP BY p.method
    """)
    fun getSalesByPaymentMethod(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Map<String, Any>>

    @Query("""
        SELECT s FROM SaleEntity s
        JOIN s.payment p
        WHERE p.transactionId = :transactionId
    """)
    fun findByPaymentTransactionId(@Param("transactionId") transactionId: String): SaleEntity?
}
