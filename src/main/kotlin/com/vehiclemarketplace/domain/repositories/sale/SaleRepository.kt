package com.vehiclemarketplace.domain.repositories.sale

import com.vehiclemarketplace.domain.model.sale.PaymentStatus
import com.vehiclemarketplace.domain.model.sale.Sale
import com.vehiclemarketplace.domain.model.sale.SaleEvent
import com.vehiclemarketplace.domain.model.sale.SaleId
import com.vehiclemarketplace.domain.model.sale.SaleStatus
import com.vehiclemarketplace.domain.model.sale.SalesSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface SaleRepository {
    suspend fun findById(id: SaleId): Sale?
    suspend fun findByBuyerId(buyerId: UUID, page: Int = 0, size: Int = 20): List<Sale>
    suspend fun findByVehicleId(vehicleId: UUID): Sale?
    suspend fun findByStatus(status: SaleStatus, page: Int = 0, size: Int = 20): List<Sale>
    suspend fun findByTransactionId(transactionId: String): Sale?

    suspend fun save(sale: Sale): Sale
    suspend fun update(sale: Sale): Sale

    suspend fun findPendingSalesOlderThan(dateTime: LocalDateTime): List<Sale>
    suspend fun findSalesByPaymentStatus(status: PaymentStatus): List<Sale>

    suspend fun getSalesSummary(startDate: LocalDate, endDate: LocalDate): SalesSummary

    suspend fun addEvent(event: SaleEvent): SaleEvent
    suspend fun findEventsBySaleId(saleId: SaleId): List<SaleEvent>
}