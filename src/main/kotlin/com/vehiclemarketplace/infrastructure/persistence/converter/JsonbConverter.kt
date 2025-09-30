package com.vehiclemarketplace.infrastructure.persistence.converter

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter

abstract class JsonbConverter<T> : AttributeConverter<T, String> {
    private val objectMapper = ObjectMapper()
    
    override fun convertToDatabaseColumn(attribute: T): String {
        return try {
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting object to JSON", e)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): T {
        if (dbData == null) {
            throw IllegalArgumentException("Database data cannot be null")
        }
        
        return try {
            objectMapper.readValue(dbData, getTypeClass())
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting JSON to object", e)
        }
    }

    abstract fun getTypeClass(): Class<T>
}
