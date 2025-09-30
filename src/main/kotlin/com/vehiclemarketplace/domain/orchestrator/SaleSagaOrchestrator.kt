package com.vehiclemarketplace.domain.orchestrator

import com.vehiclemarketplace.application.mapper.PaymentMapper.toPayment
import com.vehiclemarketplace.application.sale.dto.PaymentRequest
import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.buyer.BuyerId
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse
import com.vehiclemarketplace.domain.model.saga.SagaEvent
import com.vehiclemarketplace.domain.model.saga.SagaStatus
import com.vehiclemarketplace.domain.model.saga.SagaStep
import com.vehiclemarketplace.domain.model.saga.SaleSaga
import com.vehiclemarketplace.domain.model.sale.*
import com.vehiclemarketplace.domain.model.vehicle.Vehicle
import com.vehiclemarketplace.domain.model.vehicle.VehicleId
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import com.vehiclemarketplace.domain.orchestrator.SagaUtils.executeSagaStep
import com.vehiclemarketplace.domain.orchestrator.SagaUtils.getCompensationDataOrThrow
import com.vehiclemarketplace.domain.orchestrator.SagaUtils.updateSaleWithEvent
import com.vehiclemarketplace.domain.repositories.buyer.BuyerRepository
import com.vehiclemarketplace.domain.repositories.saga.SaleSagaRepository
import com.vehiclemarketplace.domain.repositories.sale.SaleRepository
import com.vehiclemarketplace.domain.repositories.vehicle.VehicleRepository
import com.vehiclemarketplace.domain.service.payment.PaymentService
import com.vehiclemarketplace.domain.util.PaymentUtil.updatePaymentWithGatewayResponse
import com.vehiclemarketplace.domain.util.SaleEventUtil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.*

@Component
class SaleSagaOrchestrator(
    private val sagaRepository: SaleSagaRepository,
    private val vehicleRepository: VehicleRepository,
    private val buyerRepository: BuyerRepository,
    private val saleRepository: SaleRepository,
    @Lazy private val paymentService: PaymentService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun startSale(
        vehicleId: VehicleId,
        buyerId: BuyerId,
        payment: PaymentRequest
    ): Sale {
        val saga = SaleSaga(
            saleId = UUID.randomUUID(),
            vehicleId = vehicleId,
            buyerId = buyerId,
            price = payment.amount,
            paymentMethod = payment.method,
            paymentDetails = null,
            status = SagaStatus.STARTED
        )

        sagaRepository.save(saga)

        try {
            executeSagaStep(saga, sagaRepository) {
                validateBuyer(saga)
            }

            executeSagaStep(saga, sagaRepository) {
                validateVehicle(saga)
            }

            executeSagaStep(saga, sagaRepository) {
                reserveVehicle(saga)
            }

            val buyer = getCompensationDataOrThrow<Buyer>(saga, "buyer")
            val paymentModel = toPayment(request = payment, paymentDetails = null)

            val updatedPayment = executeSagaStep(saga, sagaRepository) {
                val gatewayResponse = paymentService.createPayment(paymentModel, buyer)

                val updatedPayment = updatePaymentWithGatewayResponse(paymentModel, gatewayResponse)

                saga.addCompensationData("payment", updatedPayment)
                updatedPayment
            }

            return createSaleRecord(saga, updatedPayment)

        } catch (e: Exception) {
            handleStepFailure(saga, e)
            return recoverOrCreateCancelledSale(saga, buyerId, payment)
        }
    }

    suspend fun processPaymentConfirmation(
        transactionId: String,
        gatewayResponse: PaymentGatewayResponse
    ): Map<String, String> {
        val sale = findSaleByTransactionId(transactionId)

        if (sale != null) {
            val paymentStatus = gatewayResponse.status.let {
                when (it.toString().uppercase()) {
                    "APPROVED" -> PaymentStatus.APPROVED
                    "REJECTED", "DECLINED" -> PaymentStatus.DECLINED
                    "CANCELLED" -> PaymentStatus.CANCELLED
                    "EXPIRED" -> PaymentStatus.EXPIRED
                    "IN_PROCESS", "PENDING" -> PaymentStatus.APPROVED
                    else -> PaymentStatus.PENDING
                }
            }

            val updatedPayment = when (paymentStatus) {
                PaymentStatus.APPROVED -> {
                    val updatedPayment = sale.payment.approve(transactionId)
                    updateSaleWithEvent(
                        sale,
                        SaleStatus.PAYMENT_APPROVED,
                        updatedPayment,
                        SaleEventType.PAYMENT_PROCESSED,
                        "Payment approved with transaction ID: $transactionId"
                    )
                    updatedPayment
                }

                PaymentStatus.DECLINED -> {
                    val updatedPayment = sale.payment.decline("Payment declined by gateway")
                    updateSaleWithEvent(
                        sale,
                        SaleStatus.PAYMENT_DECLINED,
                        updatedPayment,
                        SaleEventType.PAYMENT_FAILED,
                        "Payment rejected with transaction ID: $transactionId"
                    )

                    updatedPayment
                }

                PaymentStatus.CANCELLED -> sale.payment.cancel()
                PaymentStatus.EXPIRED -> sale.payment.expire()
                else -> sale.payment.process()
            }

            val updatedSaleStatus = when (paymentStatus) {
                PaymentStatus.APPROVED -> SaleStatus.PAYMENT_APPROVED
                PaymentStatus.DECLINED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED -> SaleStatus.PAYMENT_DECLINED
                else -> sale.status
            }

            val updatedSale = sale.copy(
                status = updatedSaleStatus,
                payment = updatedPayment
            )

            if (paymentStatus == PaymentStatus.DECLINED ||
                paymentStatus == PaymentStatus.CANCELLED ||
                paymentStatus == PaymentStatus.EXPIRED
            ) {
                try {
                    val vehicle = vehicleRepository.findById(sale.vehicleId)
                    val updatedVehicle = vehicle.copy(status = VehicleStatus.AVAILABLE)
                    vehicleRepository.update(updatedVehicle)
                } catch (e: Exception) {
                    logger.error("Failed to update vehicle status: ${e.message}", e)
                }
            }

            saleRepository.update(updatedSale)
            return mapOf("status" to "processed")
        } else {
            return mapOf("status" to "no_sale_found")
        }

    }

    private suspend fun createSaleRecord(saga: SaleSaga, payment: Payment): Sale {
        val sale = Sale(
            id = saga.saleId,
            vehicleId = saga.vehicleId,
            buyerId = saga.buyerId,
            status = SaleStatus.PAYMENT_PENDING,
            payment = payment,
            events = SaleEventUtil.createEventsFromSaga(saga)
        )

        return executeSagaStep(saga, sagaRepository) {
            val savedSale = saleRepository.save(sale)
            saga.addCompensationData("sale", savedSale)
            savedSale
        }
    }

    private suspend fun recoverOrCreateCancelledSale(saga: SaleSaga, buyerId: BuyerId, payment: PaymentRequest): Sale {
        return saleRepository.findById(saga.saleId) ?: Sale(
            id = saga.saleId,
            vehicleId = saga.vehicleId,
            buyerId = buyerId,
            status = SaleStatus.CANCELLED,
            payment = Payment(
                id = UUID.randomUUID(),
                amount = payment.amount,
                method = payment.method,
                paymentDetails = null,
                status = PaymentStatus.CANCELLED
            ),
            events = SaleEventUtil.createEventsFromSaga(saga)
        )
    }

    private suspend fun findSaleByTransactionId(transactionId: String): Sale? {
        return saleRepository.findByTransactionId(transactionId)
    }

    private suspend fun validateBuyer(saga: SaleSaga) {
        val buyer = buyerRepository.findById(saga.buyerId)

        saga.addCompensationData("buyer", buyer)
    }

    private suspend fun validateVehicle(saga: SaleSaga) {
        val vehicle = vehicleRepository.findById(saga.vehicleId)

        if (vehicle.status != VehicleStatus.AVAILABLE) {
            throw IllegalStateException("Vehicle is not available for sale. Current status: ${vehicle.status}")
        }

        saga.addCompensationData("vehicle", vehicle)
    }

    private suspend fun reserveVehicle(saga: SaleSaga) {
        val vehicle = getCompensationDataOrThrow<Vehicle>(saga, "vehicle")

        val reservedVehicle = vehicle.reserve()
        vehicleRepository.update(reservedVehicle)

        saga.addCompensationData("vehicle", reservedVehicle)
    }

    private suspend fun handleStepFailure(saga: SaleSaga, error: Throwable) {
        val currentStep = saga.getCurrentStep()

        saga.markStepFailed(error.message ?: "Unknown error")

        if (currentStep != null) {
            sagaRepository.addEvent(
                saga.id,
                SagaEvent.StepFailed(
                    sagaId = saga.id,
                    step = currentStep,
                    error = error.message ?: "Unknown error"
                )
            )
        }

        saga.startCompensation()
        sagaRepository.save(saga)
        executeCompensation(saga)
    }

    private suspend fun executeCompensation(saga: SaleSaga) {
        try {
            val step = saga.getNextCompensationStep() ?: run {
                saga.status = SagaStatus.FAILED
                sagaRepository.save(saga)
                return
            }
            sagaRepository.addEvent(
                saga.id,
                SagaEvent.CompensationStarted(saga.id, step, reason = "Manual compensation")
            )

            when (step) {
                SagaStep.RESERVE_VEHICLE -> compensateReserveVehicle(saga)
                SagaStep.CREATE_PAYMENT -> compensatePayment(saga)
                else -> {}
            }

            saga.markCompensationComplete()
            sagaRepository.addEvent(saga.id, SagaEvent.CompensationCompleted(saga.id, step))

            sagaRepository.save(saga)

        } catch (e: Exception) {
            saga.status = SagaStatus.FAILED
            saga.lastError = e.message
            sagaRepository.save(saga)
        }
    }

    private suspend fun compensateReserveVehicle(saga: SaleSaga) {
        val vehicle = getCompensationDataOrThrow<Vehicle>(saga, "vehicle")

        val availableVehicle = vehicle.makeAvailable()
        vehicleRepository.update(availableVehicle)
    }

    private suspend fun compensatePayment(saga: SaleSaga) {
        val payment = getCompensationDataOrThrow<Payment>(saga, "payment")

        if (payment.status == PaymentStatus.PENDING) {
            paymentService.cancelPayment(payment.id.toString())
        }
    }
}