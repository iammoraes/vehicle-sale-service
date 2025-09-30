package com.vehiclemarketplace.domain.repositories.saga

import com.vehiclemarketplace.domain.model.saga.SagaEvent
import com.vehiclemarketplace.domain.model.saga.SaleSaga
import java.util.UUID

interface SaleSagaRepository {
    suspend fun findById(id: UUID): SaleSaga
    suspend fun save(saga: SaleSaga): SaleSaga
    suspend fun addEvent(sagaId: UUID, event: SagaEvent)
}