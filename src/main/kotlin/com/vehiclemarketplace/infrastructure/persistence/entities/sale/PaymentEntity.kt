package com.vehiclemarketplace.infrastructure.persistence.entities.sale

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vehiclemarketplace.domain.model.sale.PaymentMethod
import com.vehiclemarketplace.domain.model.sale.PaymentStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payments")
data class PaymentEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @OneToOne
    @JoinColumn(name = "sale_id", nullable = false)
    val sale: SaleEntity,

    @Column(name = "amount", nullable = false)
    val amount: BigDecimal,

    @Column(name = "currency", length = 3)
    val currency: String = "BRL",

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    val method: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "payment_date")
    val paymentDate: LocalDateTime? = null,

    @Column(name = "due_date")
    val dueDate: LocalDateTime? = null,

    @Column(name = "transaction_id", length = 500)
    val transactionId: String? = null,

    @Column(name = "payment_details", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    val paymentDetails: String, // Stored as JSON

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        private val objectMapper = jacksonObjectMapper().apply {
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        fun toJson(paymentDetails: Any): String {
            return objectMapper.writeValueAsString(paymentDetails)
        }

        internal inline fun <reified T> fromJson(json: String): T {
            return objectMapper.readValue(json)
        }
    }
}
