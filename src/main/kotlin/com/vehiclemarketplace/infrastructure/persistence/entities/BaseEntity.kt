package com.vehiclemarketplace.infrastructure.persistence.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @UpdateTimestamp
    open var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    open var deleted: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BaseEntity
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}