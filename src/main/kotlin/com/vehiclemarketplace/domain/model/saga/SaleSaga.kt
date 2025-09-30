package com.vehiclemarketplace.domain.model.saga

import com.vehiclemarketplace.domain.model.buyer.BuyerId
import com.vehiclemarketplace.domain.model.sale.PaymentDetails
import com.vehiclemarketplace.domain.model.sale.PaymentMethod
import com.vehiclemarketplace.domain.model.sale.SaleId
import com.vehiclemarketplace.domain.model.vehicle.VehicleId
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class SaleSaga(
    val id: UUID = UUID.randomUUID(),
    val saleId: SaleId,
    val vehicleId: VehicleId,
    val buyerId: BuyerId,
    val price: BigDecimal,
    val paymentMethod: PaymentMethod,
    val paymentDetails: PaymentDetails?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var status: SagaStatus = SagaStatus.STARTED,
    var currentStep: Int = 0,
    var nextStep: Int = 1,
    var compensationData: MutableMap<String, Any> = mutableMapOf(),
    var lastError: String? = null,
    val steps: List<SagaStep> = listOf(
        SagaStep.VALIDATE_BUYER,
        SagaStep.VALIDATE_VEHICLE,
        SagaStep.RESERVE_VEHICLE,
        SagaStep.CREATE_PAYMENT,
        SagaStep.UPDATE_INVENTORY,
        SagaStep.GENERATE_DOCUMENTS,
        SagaStep.NOTIFY_PARTIES
    )
) {
    internal val completedSteps: MutableList<Int> = mutableListOf()
    internal val failedSteps: MutableMap<Int, String> = mutableMapOf()
    
    fun getCurrentStep(): SagaStep? {
        return steps.getOrNull(currentStep)
    }
    
    fun moveToNextStep() {
        completedSteps.add(currentStep)
        currentStep = nextStep
        nextStep++
        
        if (currentStep >= steps.size) {
            status = SagaStatus.COMPLETED
        }
    }
    
    fun markStepFailed(error: String) {
        failedSteps[currentStep] = error
        lastError = error
        status = SagaStatus.FAILED
    }
    
    fun startCompensation() {
        status = SagaStatus.COMPENSATING
        // Reverse the completed steps for compensation
        completedSteps.reverse()
    }
    
    fun getNextCompensationStep(): SagaStep? {
        return if (completedSteps.isNotEmpty()) {
            steps[completedSteps.first()]
        } else {
            null
        }
    }
    
    fun markCompensationComplete() {
        completedSteps.remove(0)
        if (completedSteps.isEmpty()) {
            status = SagaStatus.FAILED
        }
    }
    
    fun addCompensationData(key: String, value: Any) {
        compensationData[key] = value
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> getCompensationData(key: String): T? {
        return compensationData[key] as? T
    }
}
