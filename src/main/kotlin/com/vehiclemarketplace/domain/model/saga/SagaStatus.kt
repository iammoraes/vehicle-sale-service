package com.vehiclemarketplace.domain.model.saga

enum class SagaStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    COMPENSATING,
    FAILED
}