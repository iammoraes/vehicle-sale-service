package com.vehiclemarketplace.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.vehiclemarketplace.infrastructure.security.model.AuditLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class AuditService(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun logAccess(
        userId: String,
        action: String,
        entityType: String,
        entityId: String,
        userIp: String? = null,
        userAgent: String? = null,
        reason: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val sql = """
                INSERT INTO audit_logs (entity_type, entity_id, action, user_id, user_ip, user_agent, reason, timestamp)
                VALUES (?, ?, ?, ?, ?::inet, ?, ?, ?)
            """.trimIndent()

            jdbcTemplate.update(
                sql,
                entityType,
                entityId,
                action,
                UUID.fromString(userId),
                userIp,
                userAgent,
                reason,
                LocalDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to log audit event", e)
        }
    }

    suspend fun logDataChange(
        userId: String?,
        entityType: String,
        entityId: UUID,
        action: String,
        changedFields: Map<String, Any>? = null,
        oldValues: Map<String, Any>? = null,
        newValues: Map<String, Any>? = null,
        userIp: String? = null,
        userAgent: String? = null,
        reason: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val sql = """
                INSERT INTO audit_logs (entity_type, entity_id, action, user_id, user_ip, user_agent,
                                      changed_fields, old_values, new_values, reason, timestamp)
                VALUES (?, ?, ?, ?, ?::inet, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
            """.trimIndent()

            jdbcTemplate.update(
                sql,
                entityType,
                entityId,
                action,
                userId?.let { UUID.fromString(it) },
                userIp,
                userAgent,
                changedFields?.let { objectMapper.writeValueAsString(it) },
                oldValues?.let { objectMapper.writeValueAsString(it) },
                newValues?.let { objectMapper.writeValueAsString(it) },
                reason,
                LocalDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to log data change event", e)
        }
    }

    suspend fun getAuditTrail(
        entityType: String? = null,
        entityId: UUID? = null,
        userId: UUID? = null,
        action: String? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        limit: Int = 100
    ): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        entityType?.let {
            conditions.add("entity_type = ?")
            params.add(it)
        }

        entityId?.let {
            conditions.add("entity_id = ?")
            params.add(it)
        }

        userId?.let {
            conditions.add("user_id = ?")
            params.add(it)
        }

        action?.let {
            conditions.add("action = ?")
            params.add(it)
        }

        startDate?.let {
            conditions.add("timestamp >= ?")
            params.add(it)
        }

        endDate?.let {
            conditions.add("timestamp <= ?")
            params.add(it)
        }

        val whereClause = if (conditions.isNotEmpty()) {
            "WHERE ${conditions.joinToString(" AND ")}"
        } else ""

        val sql = """
            SELECT id, entity_type, entity_id, action, user_id, user_ip, user_agent,
                   changed_fields, old_values, new_values, reason, timestamp
            FROM audit_logs
            $whereClause
            ORDER BY timestamp DESC
            LIMIT ?
        """.trimIndent()

        params.add(limit)

        jdbcTemplate.query(sql, params.toTypedArray()) { rs, _ ->
            AuditLogEntry(
                id = UUID.fromString(rs.getString("id")),
                entityType = rs.getString("entity_type"),
                entityId = UUID.fromString(rs.getString("entity_id")),
                action = rs.getString("action"),
                userId = rs.getString("user_id")?.let { UUID.fromString(it) },
                userIp = rs.getString("user_ip"),
                userAgent = rs.getString("user_agent"),
                changedFields = rs.getString("changed_fields")?.let {
                    objectMapper.readValue(it, Map::class.java) as Map<String, Any>
                },
                oldValues = rs.getString("old_values")?.let {
                    objectMapper.readValue(it, Map::class.java) as Map<String, Any>
                },
                newValues = rs.getString("new_values")?.let {
                    objectMapper.readValue(it, Map::class.java) as Map<String, Any>
                },
                reason = rs.getString("reason"),
                timestamp = rs.getTimestamp("timestamp").toLocalDateTime()
            )
        }
    }
}
