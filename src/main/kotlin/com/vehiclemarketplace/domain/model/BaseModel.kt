package com.vehiclemarketplace.domain.model

import java.io.Serializable
import java.time.LocalDateTime

abstract class BaseModel<T : Serializable>(
    open val id: T? = null,
    open val createdAt: LocalDateTime = LocalDateTime.now(),
    open val updatedAt: LocalDateTime = LocalDateTime.now(),
    open val version: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseModel<*>

        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}