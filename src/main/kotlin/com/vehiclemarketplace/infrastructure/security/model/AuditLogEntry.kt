package com.vehiclemarketplace.infrastructure.security.model

import java.time.LocalDateTime
import java.util.UUID

data class AuditLogEntry(
    val id: UUID,
    val entityType: String,
    val entityId: UUID,
    val action: String,
    val userId: UUID?,
    val userIp: String?,
    val userAgent: String?,
    val changedFields: Map<String, Any>?,
    val oldValues: Map<String, Any>?,
    val newValues: Map<String, Any>?,
    val reason: String?,
    val timestamp: LocalDateTime
)
