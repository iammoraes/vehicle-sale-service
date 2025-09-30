package com.vehiclemarketplace.application.sale

import com.vehiclemarketplace.application.sale.dto.SaleRequest
import com.vehiclemarketplace.application.sale.dto.SaleResponse
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse
import com.vehiclemarketplace.domain.model.sale.SaleStatus
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import com.vehiclemarketplace.domain.orchestrator.SaleSagaOrchestrator
import com.vehiclemarketplace.domain.repositories.buyer.BuyerRepository
import com.vehiclemarketplace.domain.repositories.sale.SaleRepository
import com.vehiclemarketplace.domain.repositories.vehicle.VehicleRepository
import com.vehiclemarketplace.domain.service.sale.SaleService
import com.vehiclemarketplace.infrastructure.exception.SagaExecutionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class SaleServiceImpl(
    private val sagaOrchestrator: SaleSagaOrchestrator,
    private val saleRepository: SaleRepository,
    private val vehicleRepository: VehicleRepository,
    private val buyerRepository: BuyerRepository
) : SaleService {

    override suspend fun initiateSale(request: SaleRequest): SaleResponse =
        withContext(Dispatchers.IO) {
            try {
                val sale = sagaOrchestrator.startSale(
                    vehicleId = request.vehicleId,
                    buyerId = request.buyerId,
                    payment = request.payment
                )

                SaleResponse.fromDomain(sale)
            } catch (e: Exception) {
                throw SagaExecutionException("Failed to initiate sale: ${e.message}", e)
            }
        }

    override suspend fun cancelSale(saleId: UUID, reason: String): SaleResponse? {
        return withContext(Dispatchers.IO) {
            val sale = saleRepository.findById(saleId)
                ?: return@withContext null

            if (sale.status in listOf(SaleStatus.COMPLETED, SaleStatus.CANCELLED)) {
                throw IllegalArgumentException("Cannot cancel sale in status: ${sale.status}")
            }


            val vehicle = vehicleRepository.findById(sale.vehicleId)

            val updatedVehicle = vehicle.copy(
                status = VehicleStatus.AVAILABLE
            )

            vehicleRepository.update(updatedVehicle)

            val cancelledSale = sale.cancel(reason)
            saleRepository.update(cancelledSale)

            SaleResponse.fromDomain(cancelledSale)
        }
    }

    override suspend fun confirmDelivery(saleId: UUID): SaleResponse? =
        withContext(Dispatchers.IO) {
            val sale = saleRepository.findById(saleId)
                ?: return@withContext null

            if (sale.status != SaleStatus.PAYMENT_APPROVED) {
                throw IllegalArgumentException("Delivery confirmation not allowed for status: ${sale.status}")
            }

            val vehicle = vehicleRepository.findById(sale.vehicleId)
            val buyer = buyerRepository.findById(sale.buyerId)
            val completedSale = sale.copy(
                status = SaleStatus.COMPLETED,
                updatedAt = LocalDateTime.now()
            )

            val updatedVehicle = vehicle.copy(
                status = VehicleStatus.SOLD
            )

            vehicleRepository.update(updatedVehicle)

            saleRepository.update(completedSale)

            SaleResponse.fromDomain(completedSale, updatedVehicle, buyer)
        }

    override suspend fun updateSale(
        transactionId: String,
        paymentGatewayResponse: PaymentGatewayResponse
    ): Map<String, String> {
        return sagaOrchestrator.processPaymentConfirmation(transactionId, paymentGatewayResponse)
    }
}
