package com.vehiclemarketplace.infrastructure.persistence.entities.sale

import com.vehiclemarketplace.domain.model.sale.SaleEventType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "sale_events")
data class SaleEventEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "sale_id", nullable = false)
    val saleId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: SaleEventType,

    @Column(name = "payload", nullable = false)
    val payload: String,

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)
