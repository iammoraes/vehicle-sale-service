package com.vehiclemarketplace.infrastructure.persistence.repositories.saga

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vehiclemarketplace.domain.model.saga.SagaEvent
import com.vehiclemarketplace.domain.model.saga.SagaStatus
import com.vehiclemarketplace.domain.model.saga.SagaStep
import com.vehiclemarketplace.domain.model.saga.SaleSaga
import com.vehiclemarketplace.domain.repositories.saga.SaleSagaRepository
import com.vehiclemarketplace.infrastructure.persistence.entities.saga.SaleSagaEntity
import com.vehiclemarketplace.infrastructure.persistence.jpa.saga.JpaSaleSagaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class SaleSagaRepositoryImpl(
    private val jpaSagaRepository: JpaSaleSagaRepository,
    private val objectMapper: ObjectMapper
) : SaleSagaRepository {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun findById(id: UUID): SaleSaga = withContext(Dispatchers.IO) {
        jpaSagaRepository.findById(id).orElseThrow { throw IllegalArgumentException("Saga not found: $id") }
            .toDomain(objectMapper)
    }

    override suspend fun save(saga: SaleSaga): SaleSaga = withContext(Dispatchers.IO) {
        try {
            val entity = SaleSagaEntity.fromDomain(saga, objectMapper)
            val savedEntity = jpaSagaRepository.save(entity)
            return@withContext savedEntity.toDomain(objectMapper)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun addEvent(sagaId: UUID, event: SagaEvent): Unit = withContext(Dispatchers.IO) {
        try {
            val sagaEntity = jpaSagaRepository.findById(sagaId).orElse(null)
                ?: throw IllegalArgumentException("Saga not found: $sagaId")

            when (event) {
                is SagaEvent.StepCompleted -> {
                    val completedSteps = parseCompletedSteps(sagaEntity.completedSteps)

                    val stepIndex = getStepIndex(event.step, sagaEntity.steps)

                    if (stepIndex != -1 && !completedSteps.contains(stepIndex)) {
                        completedSteps.add(stepIndex)
                    }

                    sagaEntity.completedSteps = objectMapper.writeValueAsString(completedSteps)
                }

                is SagaEvent.StepFailed -> {
                    sagaEntity.lastError = event.error
                    val failedSteps = parseFailedSteps(sagaEntity.failedSteps)
                    val stepIndex = getStepIndex(event.step, sagaEntity.steps)
                    if (stepIndex != -1) {
                        failedSteps[stepIndex.toString()] = event.error
                    }

                    sagaEntity.failedSteps = objectMapper.writeValueAsString(failedSteps)
                    sagaEntity.status = SagaStatus.FAILED
                }

                is SagaEvent.CompensationStarted -> {
                    sagaEntity.status = SagaStatus.COMPENSATING
                }

                is SagaEvent.CompensationCompleted -> {
                    val completedSteps = parseCompletedSteps(sagaEntity.completedSteps)
                    val stepIndex = getStepIndex(event.step, sagaEntity.steps)

                    if (stepIndex != -1) {
                        completedSteps.remove(Integer.valueOf(stepIndex))
                    }

                    sagaEntity.completedSteps = objectMapper.writeValueAsString(completedSteps)
                }

                is SagaEvent.SagaCompleted -> {
                    sagaEntity.status = SagaStatus.COMPLETED
                }

                is SagaEvent.SagaFailed -> {
                    sagaEntity.lastError = event.error
                    sagaEntity.status = SagaStatus.FAILED
                }

                else -> {}
            }

            jpaSagaRepository.save(sagaEntity)

        } catch (e: Exception) {
            throw e
        }
    }

    private fun parseCompletedSteps(completedStepsJson: String): MutableList<Int> {
        return try {
            if (completedStepsJson.isBlank() || completedStepsJson == "[]") {
                mutableListOf()
            } else {
                objectMapper.readValue<Array<Int>>(completedStepsJson).toMutableList()
            }
        } catch (e: Exception) {
            logger.error("Error parsing completed steps: $completedStepsJson", e)
            mutableListOf()
        }
    }

    private fun parseFailedSteps(failedStepsJson: String): MutableMap<String, String> {
        return try {
            if (failedStepsJson.isBlank() || failedStepsJson == "{}") {
                mutableMapOf()
            } else {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(failedStepsJson, Map::class.java) as MutableMap<String, String>
            }
        } catch (e: Exception) {
            logger.error("Error parsing failed steps: $failedStepsJson", e)
            mutableMapOf()
        }
    }

    private fun getStepIndex(step: SagaStep, stepsJson: String): Int {
        return try {
            val steps = objectMapper.readValue<Array<String>>(stepsJson)
            steps.indexOf(step.name)
        } catch (e: Exception) {
            logger.error("Error getting step index for step ${step.name} in $stepsJson", e)
            -1
        }
    }
}
