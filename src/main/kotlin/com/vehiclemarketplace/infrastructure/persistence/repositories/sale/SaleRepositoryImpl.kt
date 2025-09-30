package com.vehiclemarketplace.infrastructure.persistence.repositories.sale

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.vehiclemarketplace.domain.model.sale.*
import com.vehiclemarketplace.domain.repositories.sale.SaleRepository
import com.vehiclemarketplace.infrastructure.persistence.entities.sale.PaymentEntity
import com.vehiclemarketplace.infrastructure.persistence.entities.sale.SaleEntity
import com.vehiclemarketplace.infrastructure.persistence.entities.sale.SaleEventEntity
import com.vehiclemarketplace.infrastructure.persistence.jpa.sale.JpaSaleEventRepository
import com.vehiclemarketplace.infrastructure.persistence.jpa.sale.JpaSaleRepository
import com.vehiclemarketplace.infrastructure.security.SensitiveDataProtectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Repository
class SaleRepositoryImpl(
    private val jpaSaleRepository: JpaSaleRepository,
    private val jpaSaleEventRepository: JpaSaleEventRepository,
    private val sensitiveDataProtectionService: SensitiveDataProtectionService
) : SaleRepository {

    override suspend fun findById(id: SaleId): Sale? = withContext(Dispatchers.IO) {
        jpaSaleRepository.findById(id).orElse(null)?.toDomain()
    }

    override suspend fun findByBuyerId(buyerId: UUID, page: Int, size: Int): List<Sale> =
        withContext(Dispatchers.IO) {
            val pageable = PageRequest.of(page, size)
            jpaSaleRepository.findByBuyerId(buyerId, pageable).map { it.toDomain() }
        }

    override suspend fun findByVehicleId(vehicleId: UUID): Sale? = withContext(Dispatchers.IO) {
        jpaSaleRepository.findByVehicleId(vehicleId)?.toDomain()
    }

    override suspend fun findByStatus(status: SaleStatus, page: Int, size: Int): List<Sale> =
        withContext(Dispatchers.IO) {
            val pageable = PageRequest.of(page, size)
            jpaSaleRepository.findByStatus(status, pageable).map { it.toDomain() }
        }

    override suspend fun findByTransactionId(transactionId: String): Sale? = withContext(Dispatchers.IO) {
        jpaSaleRepository.findByPaymentTransactionId(transactionId)?.toDomain()
    }

    override suspend fun save(sale: Sale): Sale = withContext(Dispatchers.IO) {
        val saleEntity = sale.toEntity()
        val savedEntity = jpaSaleRepository.save(saleEntity)

        sale.events.forEach { event ->
            val eventEntity = SaleEventEntity(
                id = event.id,
                saleId = sale.id,
                eventType = event.eventType,
                payload = event.payload,
                timestamp = event.timestamp
            )
            jpaSaleEventRepository.save(eventEntity)
        }

        savedEntity.toDomain()
    }

    override suspend fun update(sale: Sale): Sale = withContext(Dispatchers.IO) {
        sale.id.let { jpaSaleRepository.findById(it).orElse(null) }
            ?: throw IllegalArgumentException("Cannot update non-existent sale")

        val updatedEntity = jpaSaleRepository.save(sale.toEntity())
        updatedEntity.toDomain()
    }

    override suspend fun findPendingSalesOlderThan(dateTime: LocalDateTime): List<Sale> =
        withContext(Dispatchers.IO) {
            jpaSaleRepository.findPendingSalesOlderThan(dateTime).map { it.toDomain() }
        }

    override suspend fun findSalesByPaymentStatus(status: PaymentStatus): List<Sale> =
        withContext(Dispatchers.IO) {
            jpaSaleRepository.findSalesByPaymentStatus(status).map { it.toDomain() }
        }

    override suspend fun getSalesSummary(startDate: LocalDate, endDate: LocalDate): SalesSummary =
        withContext(Dispatchers.IO) {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(LocalTime.MAX)

            val basicSummary = jpaSaleRepository.getSalesSummaryBasic(startDateTime, endDateTime)

            val salesByStatus = jpaSaleRepository.getSalesByStatus(startDateTime, endDateTime)
                .associate {
                    val status = it["status"] as String
                    val count = (it["count"] as Number).toInt()
                    SaleStatus.valueOf(status) to count
                }

            val salesByPaymentMethod = jpaSaleRepository.getSalesByPaymentMethod(startDateTime, endDateTime)
                .associate {
                    val method = it["method"] as String
                    val count = (it["count"] as Number).toInt()
                    PaymentMethod.valueOf(method) to count
                }

            SalesSummary(
                totalSales = (basicSummary["totalSales"] as Number).toInt(),
                totalRevenue = (basicSummary["totalRevenue"] as BigDecimal),
                totalVehiclesSold = (basicSummary["totalVehiclesSold"] as Number).toInt(),
                averageSaleValue = if ((basicSummary["totalSales"] as Number).toInt() > 0) {
                    (basicSummary["totalRevenue"] as BigDecimal)
                        .divide(BigDecimal((basicSummary["totalSales"] as Number).toInt()), 2, RoundingMode.HALF_UP)
                } else BigDecimal.ZERO,
                salesByStatus = salesByStatus,
                salesByPaymentMethod = salesByPaymentMethod,
                salesByVehicleBrand = emptyMap(),
                salesByMonth = emptyMap()
            )
        }

    override suspend fun addEvent(event: SaleEvent) = withContext(Dispatchers.IO) {
        val eventEntity = SaleEventEntity(
            id = event.id,
            saleId = event.saleId,
            eventType = event.eventType,
            payload = event.payload,
            timestamp = event.timestamp
        )
        jpaSaleEventRepository.save(eventEntity)
        event
    }

    override suspend fun findEventsBySaleId(saleId: SaleId): List<SaleEvent> =
        withContext(Dispatchers.IO) {
            jpaSaleEventRepository.findBySaleIdOrderByTimestampAsc(saleId)
                .map {
                    SaleEvent(
                        id = it.id,
                        saleId = it.saleId,
                        eventType = it.eventType,
                        payload = it.payload,
                        timestamp = it.timestamp
                    )
                }
        }

    private fun SaleEntity.toDomain(): Sale {
        val domainPayment = this.payment?.let { paymentEntity ->
            val mapper = ObjectMapper()
            
            val decryptedPaymentDetails = sensitiveDataProtectionService.revealPaymentDetails(paymentEntity.paymentDetails)
            
            val map: MutableMap<String, String> =
                mapper.readValue(decryptedPaymentDetails, object : TypeReference<MutableMap<String, String>>() {})

            val paymentDetails = when (paymentEntity.method) {
                PaymentMethod.PIX -> PixDetails(
                    pixKey = map["pixKey"] ?: "",
                    expirationDate = map["expirationDate"]?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
                )

                PaymentMethod.BOLETO -> BoletoDetails(
                    barcode = map["barcode"] ?: "",
                    dueDate = map["dueDate"]?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
                    pdfUrl = map["pdfUrl"] ?: "")
            }

            Payment(
                id = paymentEntity.id,
                amount = paymentEntity.amount,
                currency = paymentEntity.currency,
                method = paymentEntity.method,
                status = paymentEntity.status,
                paymentDate = paymentEntity.paymentDate,
                dueDate = paymentEntity.dueDate,
                paymentDetails = paymentDetails,
                transactionId = paymentEntity.transactionId,
            )
        } ?: throw IllegalStateException("Sale must have a payment")


        return Sale(
            id = this.id,
            vehicleId = this.vehicleId,
            buyerId = this.buyerId,
            status = this.status,
            payment = domainPayment,
            notes = this.notes,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
        )
    }

    private fun Sale.toEntity(): SaleEntity {
        val paymentDetailsJson = when {
            payment.paymentDetails == null -> "{}"
            payment.method == PaymentMethod.PIX -> PaymentEntity.toJson(payment.paymentDetails)
            payment.method == PaymentMethod.BOLETO -> PaymentEntity.toJson(payment.paymentDetails)
            else -> throw IllegalArgumentException("Unsupported payment details type: ${payment.method}")
        }

        val baseSaleEntity = SaleEntity(
            id = this.id,
            vehicleId = this.vehicleId,
            buyerId = this.buyerId,
            status = this.status,
            notes = this.notes,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            payment = null
        )

        val paymentEntity = PaymentEntity(
            id = this.payment.id,
            sale = baseSaleEntity,
            amount = this.payment.amount,
            currency = this.payment.currency,
            method = this.payment.method,
            status = this.payment.status,
            paymentDate = this.payment.paymentDate,
            dueDate = this.payment.dueDate,
            transactionId = this.payment.transactionId,
            paymentDetails = paymentDetailsJson
        )

        val encryptedPaymentDetails = sensitiveDataProtectionService.protectPaymentDetails(
            payment = paymentEntity,
            originalDetails = paymentDetailsJson
        )

        val encryptedPaymentEntity = paymentEntity.copy(
            paymentDetails = encryptedPaymentDetails
        )

        return baseSaleEntity.copy(payment = encryptedPaymentEntity)
    }
}