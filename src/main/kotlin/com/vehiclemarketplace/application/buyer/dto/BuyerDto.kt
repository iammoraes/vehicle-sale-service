package com.vehiclemarketplace.application.buyer.dto

import com.vehiclemarketplace.domain.model.buyer.Address
import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.buyer.Document
import com.vehiclemarketplace.domain.model.buyer.DocumentType
import java.time.LocalDate
import java.util.*

data class BuyerDto(
    val id: UUID? = null,
    val name: String,
    val email: String,
    val phone: String,
    val birthDate: LocalDate,
    val address: AddressDto,
    val documents: List<DocumentDto> = emptyList()
) {
    fun toDomain() = Buyer(
        id = id,
        name = name,
        email = email,
        phone = phone,
        birthDate = birthDate,
        address = address.toDomain(),
        documents = documents.map { it.toDomain() }
    )
    
    companion object {
        fun fromDomain(buyer: Buyer) = BuyerDto(
            id = buyer.id,
            name = buyer.name,
            email = buyer.email,
            phone = buyer.phone,
            birthDate = buyer.birthDate,
            address = AddressDto.fromDomain(buyer.address),
            documents = buyer.documents.map { DocumentDto.fromDomain(it) }
        )
    }
}

data class AddressDto(
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
) {
    fun toDomain() = Address(
        street = street,
        number = number,
        complement = complement,
        neighborhood = neighborhood,
        city = city,
        state = state,
        postalCode = postalCode,
        country = country
    )
    
    companion object {
        fun fromDomain(address: Address) = AddressDto(
            street = address.street,
            number = address.number,
            complement = address.complement,
            neighborhood = address.neighborhood,
            city = address.city,
            state = address.state,
            postalCode = address.postalCode,
            country = address.country
        )
    }
}

data class DocumentDto(
    val type: DocumentType,
    val number: String
) {
    fun toDomain() = Document(
        type = type,
        number = number
    )
    
    companion object {
        fun fromDomain(document: Document) = DocumentDto(
            type = document.type,
            number = document.number
        )
    }
}
