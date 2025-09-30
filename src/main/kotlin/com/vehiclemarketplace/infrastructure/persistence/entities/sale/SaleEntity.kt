package com.vehiclemarketplace.infrastructure.persistence.entities.sale

import com.vehiclemarketplace.domain.model.sale.SaleStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "sales")
data class SaleEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "vehicle_id", nullable = false)
    val vehicleId: UUID,

    @Column(name = "buyer_id", nullable = false)
    val buyerId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: SaleStatus,

    @Column(name = "notes")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // Associated payments
    @OneToOne(mappedBy = "sale", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val payment: PaymentEntity? = null,

    // Associated events
    @OneToMany(mappedBy = "saleId", cascade = [CascadeType.ALL], orphanRemoval = true)
    val events: MutableList<SaleEventEntity> = mutableListOf()
)
