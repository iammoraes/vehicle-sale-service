package com.vehiclemarketplace.domain.orchestrator

import com.vehiclemarketplace.domain.model.saga.SaleSaga
import com.vehiclemarketplace.domain.model.sale.*
import com.vehiclemarketplace.domain.repositories.saga.SaleSagaRepository

object SagaUtils {

    suspend fun <T> executeSagaStep(
        saga: SaleSaga,
        sagaRepository: SaleSagaRepository,
        action: suspend () -> T
    ): T {
        val result = action()
        saga.moveToNextStep()
        sagaRepository.save(saga)
        return result
    }

    inline fun <reified T> getCompensationDataOrThrow(saga: SaleSaga, key: String): T {
        return saga.getCompensationData<T>(key)
            ?: throw IllegalStateException("${T::class.simpleName} data not found in saga")
    }

    fun updateSaleWithEvent(
        sale: Sale,
        status: SaleStatus,
        payment: Payment,
        eventType: SaleEventType,
        eventPayload: String
    ): Sale {
        val newEvents = ArrayList(sale.events)
        newEvents.add(
            SaleEvent(
                saleId = sale.id,
                eventType = eventType,
                payload = eventPayload
            )
        )

        return Sale(
            id = sale.id,
            vehicleId = sale.vehicleId,
            buyerId = sale.buyerId,
            status = status,
            payment = payment,
            events = newEvents
        )
    }
}
