package com.vehiclemarketplace.domain.util

import com.vehiclemarketplace.domain.model.saga.SagaStatus
import com.vehiclemarketplace.domain.model.saga.SagaStep
import com.vehiclemarketplace.domain.model.saga.SaleSaga
import com.vehiclemarketplace.domain.model.sale.Payment
import com.vehiclemarketplace.domain.model.sale.PaymentStatus
import com.vehiclemarketplace.domain.model.sale.SaleEvent
import com.vehiclemarketplace.domain.model.sale.SaleEventType

object SaleEventUtil {

    fun createEventsFromSaga(saga: SaleSaga): MutableList<SaleEvent> {
        val events = mutableListOf<SaleEvent>()

        events.add(createSaleCreatedEvent(saga))

        for (stepIndex in saga.completedSteps) {
            val step = saga.steps.getOrNull(stepIndex) ?: continue
            
            when (step) {
                SagaStep.RESERVE_VEHICLE -> events.add(createVehicleReservedEvent(saga))
                SagaStep.CREATE_PAYMENT -> {
                    events.add(createPaymentInitiatedEvent(saga))
                    addPaymentResultEvent(events, saga)
                }
                else -> {}
            }
        }

        addFinalStatusEvent(events, saga)

        return events
    }

    private fun createSaleCreatedEvent(saga: SaleSaga): SaleEvent {
        return SaleEvent(
            saleId = saga.saleId,
            eventType = SaleEventType.SALE_CREATED,
            payload = "Sale created for vehicle ${saga.vehicleId} by buyer ${saga.buyerId}"
        )
    }

    private fun createVehicleReservedEvent(saga: SaleSaga): SaleEvent {
        return SaleEvent(
            saleId = saga.saleId,
            eventType = SaleEventType.VEHICLE_RESERVED,
            payload = "Vehicle ${saga.vehicleId} reserved for sale"
        )
    }

    private fun createPaymentInitiatedEvent(saga: SaleSaga): SaleEvent {
        return SaleEvent(
            saleId = saga.saleId,
            eventType = SaleEventType.PAYMENT_INITIATED,
            payload = "Payment initiated with method ${saga.paymentMethod}"
        )
    }

    private fun addPaymentResultEvent(events: MutableList<SaleEvent>, saga: SaleSaga) {
        saga.getCompensationData<Payment>("payment")?.let { payment ->
            val eventType = when (payment.status) {
                PaymentStatus.APPROVED -> SaleEventType.PAYMENT_PROCESSED
                PaymentStatus.DECLINED, 
                PaymentStatus.CANCELLED,
                PaymentStatus.EXPIRED -> SaleEventType.PAYMENT_FAILED
                PaymentStatus.PENDING -> SaleEventType.PAYMENT_PENDING
                else -> null
            }

            eventType?.let {
                val message = when (payment.status) {
                    PaymentStatus.APPROVED -> "Payment processed successfully with ID: ${payment.transactionId ?: "N/A"}"
                    PaymentStatus.DECLINED -> "Payment declined"
                    PaymentStatus.CANCELLED -> "Payment cancelled by user"
                    PaymentStatus.EXPIRED -> "Payment expired - time limit exceeded"
                    PaymentStatus.PENDING -> "Payment pending"
                    else -> "Payment in status: ${payment.status}"
                }

                events.add(
                    SaleEvent(
                        saleId = saga.saleId,
                        eventType = eventType,
                        payload = message
                    )
                )
            }
        }
    }

    private fun addFinalStatusEvent(events: MutableList<SaleEvent>, saga: SaleSaga) {
        when (saga.status) {
            SagaStatus.COMPLETED -> {
                events.add(
                    SaleEvent(
                        saleId = saga.saleId,
                        eventType = SaleEventType.SALE_COMPLETED,
                        payload = "Sale process completed successfully"
                    )
                )
            }
            SagaStatus.FAILED -> {
                events.add(
                    SaleEvent(
                        saleId = saga.saleId,
                        eventType = SaleEventType.SALE_CANCELLED,
                        payload = "Sale cancelled: ${saga.lastError ?: "Unknown error"}"
                    )
                )
            }
            else -> {}
        }
    }
}
