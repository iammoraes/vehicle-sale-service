package com.vehiclemarketplace.domain.model.buyer

import com.vehiclemarketplace.domain.model.BaseModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

typealias BuyerId = UUID

data class Buyer(
    override val id: BuyerId? = null,
    val name: String,
    val email: String,
    val phone: String,
    val birthDate: LocalDate,
    val address: Address,
    val documents: List<Document> = emptyList(),
    override val createdAt: LocalDateTime = LocalDateTime.now(),
    override val updatedAt: LocalDateTime = LocalDateTime.now(),
) : BaseModel<BuyerId>(id, createdAt, updatedAt)

data class Address(
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

data class Document(
    val type: DocumentType,
    val number: String
)

enum class DocumentType {
    CPF,
    CNH,
    RG
}
