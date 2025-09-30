package com.vehiclemarketplace.infrastructure.persistence.entities.saga

import com.fasterxml.jackson.databind.ObjectMapper
import com.vehiclemarketplace.domain.model.saga.SagaStatus
import com.vehiclemarketplace.domain.model.saga.SagaStep
import com.vehiclemarketplace.domain.model.saga.SaleSaga
import com.vehiclemarketplace.domain.model.sale.PaymentDetails
import com.vehiclemarketplace.domain.model.sale.PaymentMethod
import jakarta.persistence.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "sale_sagas")
class SaleSagaEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "sale_id", nullable = false)
    val saleId: UUID,

    @Column(name = "vehicle_id", nullable = false)
    val vehicleId: UUID,

    @Column(name = "buyer_id", nullable = false)
    val buyerId: UUID,

    @Column(name = "price", nullable = false)
    val price: BigDecimal,

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    val paymentMethod: PaymentMethod,

    @Column(name = "payment_details", nullable = false, columnDefinition = "TEXT")
    val paymentDetails: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: SagaStatus = SagaStatus.STARTED,

    @Column(name = "current_step")
    var currentStep: Int = 0,

    @Column(name = "next_step")
    var nextStep: Int = 1,

    @Column(name = "compensation_data", columnDefinition = "TEXT")
    var compensationData: String = "{}",

    @Column(name = "last_error")
    var lastError: String? = null,

    @Column(name = "retry_count")
    var retryCount: Int = 0,

    @Column(name = "max_retries")
    val maxRetries: Int = 3,

    @Column(name = "steps", columnDefinition = "TEXT")
    val steps: String,

    @Column(name = "completed_steps", columnDefinition = "TEXT")
    var completedSteps: String = "[]", // JSON serialized completed steps

    @Column(name = "failed_steps", columnDefinition = "TEXT")
    var failedSteps: String = "{}" // JSON serialized failed steps
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SaleSagaEntity::class.java)
        
        fun fromDomain(saga: SaleSaga, objectMapper: ObjectMapper): SaleSagaEntity {
            try {
                // Use ObjectMapper for proper JSON serialization
                val stepsJson = objectMapper.writeValueAsString(saga.steps.map { it.name })
                val completedStepsJson = objectMapper.writeValueAsString(saga.completedSteps)
                val failedStepsJson = objectMapper.writeValueAsString(saga.failedSteps)
                val paymentDetailsJson = objectMapper.writeValueAsString(saga.paymentDetails)
                val compensationDataJson = objectMapper.writeValueAsString(saga.compensationData)

                return SaleSagaEntity(
                    id = saga.id,
                    saleId = saga.saleId,
                    vehicleId = saga.vehicleId,
                    buyerId = saga.buyerId,
                    price = saga.price,
                    paymentMethod = saga.paymentMethod,
                    paymentDetails = paymentDetailsJson,
                    createdAt = saga.createdAt,
                    status = saga.status,
                    currentStep = saga.currentStep,
                    nextStep = saga.nextStep,
                    compensationData = compensationDataJson,
                    lastError = saga.lastError,
                    steps = stepsJson,
                    completedSteps = completedStepsJson,
                    failedSteps = failedStepsJson
                )
            } catch (e: Exception) {
                logger.error("Error serializing SaleSaga to entity", e)
                throw e
            }
        }
    }

    // Convert entity to domain model using ObjectMapper
    fun toDomain(objectMapper: ObjectMapper): SaleSaga {
        try {
            // Deserialize steps
            val stepsList = try {
                val stepNames = objectMapper.readValue(steps, Array<String>::class.java).toList()
                stepNames.map { SagaStep.valueOf(it) }
            } catch (e: Exception) {
                logger.error("Error deserializing steps: $steps", e)
                emptyList()
            }
                
            // Deserialize completed steps
            val completedStepsList = try {
                objectMapper.readValue(completedSteps, Array<Int>::class.java).toMutableList()
            } catch (e: Exception) {
                logger.error("Error deserializing completed steps: $completedSteps", e)
                mutableListOf()
            }
                
            // Deserialize failed steps
            val failedStepsMap = try {
                @Suppress("UNCHECKED_CAST")
                val map = objectMapper.readValue(failedSteps, Map::class.java) as Map<*, *>
                map.entries.associate { 
                    val key = try { (it.key as String).toInt() } catch (e: Exception) { it.key.toString().toInt() }
                    val value = it.value?.toString() ?: ""
                    key to value 
                }.toMutableMap()
            } catch (e: Exception) {
                logger.error("Error deserializing failed steps: $failedSteps", e)
                mutableMapOf()
            }
            
            // Deserialize payment details
            val paymentDetailsObj = try {
                objectMapper.readValue(paymentDetails, PaymentDetails::class.java)
            } catch (e: Exception) {
                logger.error("Error deserializing payment details: $paymentDetails", e)
                null
            }
            
            // Deserialize compensation data
            val compensationDataMap = try {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(compensationData, Map::class.java) as MutableMap<String, Any>
            } catch (e: Exception) {
                logger.error("Error deserializing compensation data: $compensationData", e)
                mutableMapOf<String, Any>()
            }
            
            // Create the saga instance
            val saga = SaleSaga(
                id = id,
                saleId = saleId,
                vehicleId = vehicleId,
                buyerId = buyerId,
                price = price,
                paymentMethod = paymentMethod,
                paymentDetails = paymentDetailsObj,
                createdAt = createdAt,
                status = status,
                currentStep = currentStep,
                nextStep = nextStep,
                compensationData = compensationDataMap,
                lastError = lastError,
                steps = stepsList
            )
            
            // Set completed steps
            saga.completedSteps.addAll(completedStepsList)
            
            // Set failed steps
            failedStepsMap.forEach { (key, value) ->
                saga.failedSteps[key] = value
            }
            
            return saga
        } catch (e: Exception) {
            logger.error("Error deserializing entity to SaleSaga", e)
            throw e
        }
    }
}
