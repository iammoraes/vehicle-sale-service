package com.vehiclemarketplace.domain.model.saga

import java.time.LocalDateTime
import java.util.UUID

sealed class SagaEvent {
    abstract val sagaId: UUID
    abstract val timestamp: LocalDateTime
    abstract val step: SagaStep

    data class StepStarted(
        override val sagaId: UUID,
        override val step: SagaStep,
        override val timestamp: LocalDateTime = LocalDateTime.now(),
        val data: Map<String, Any> = emptyMap()
    ) : SagaEvent()

    data class StepCompleted(
        override val sagaId: UUID,
        override val step: SagaStep,
        override val timestamp: LocalDateTime = LocalDateTime.now(),
        val result: Map<String, Any> = emptyMap()
    ) : SagaEvent()

    data class StepFailed(
        override val sagaId: UUID,
        override val step: SagaStep,
        val error: String,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : SagaEvent()

    data class CompensationStarted(
        override val sagaId: UUID,
        override val step: SagaStep,
        override val timestamp: LocalDateTime = LocalDateTime.now(),
        val reason: String
    ) : SagaEvent()

    data class CompensationCompleted(
        override val sagaId: UUID,
        override val step: SagaStep,
        override val timestamp: LocalDateTime = LocalDateTime.now(),
        val result: Map<String, Any> = emptyMap()
    ) : SagaEvent()

    data class CompensationFailed(
        override val sagaId: UUID,
        override val step: SagaStep,
        val error: String,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : SagaEvent()

    data class SagaCompleted(
        override val sagaId: UUID,
        override val step: SagaStep,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : SagaEvent()

    data class SagaFailed(
        override val sagaId: UUID,
        override val step: SagaStep,
        val error: String,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : SagaEvent()
}