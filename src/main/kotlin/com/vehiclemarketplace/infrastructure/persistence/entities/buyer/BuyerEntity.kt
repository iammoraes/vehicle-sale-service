package com.vehiclemarketplace.infrastructure.persistence.entities.buyer

import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.infrastructure.persistence.entities.BaseEntity
import jakarta.persistence.*
import lombok.ToString
import java.time.LocalDate

@Entity
@Table(name = "buyers")
class BuyerEntity(
    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var phone: String,

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate,

    @Column(nullable = false)
    var address: String,

    @OneToOne(mappedBy = "buyer", cascade = [CascadeType.ALL])
    @ToString.Exclude
    var addressEntity: AddressEntity,

    @OneToMany(mappedBy = "buyer", cascade = [CascadeType.ALL])
    @ToString.Exclude
    var documents: List<DocumentEntity>

) : BaseEntity() {

    override fun toString(): String {
        return "BuyerEntity(id=$id, name=$name, email=$email, phone=$phone, birthDate=$birthDate)"
    }

    companion object {
        fun fromDomain(buyer: Buyer): BuyerEntity {
            val addressEntity = AddressEntity.fromDomain(buyer.address)
            val entity = BuyerEntity(
                name = buyer.name,
                email = buyer.email,
                phone = buyer.phone,
                birthDate = buyer.birthDate,
                address = "${buyer.address.street}, ${buyer.address.number} - ${buyer.address.neighborhood}, ${buyer.address.city}, ${buyer.address.state}", 
                addressEntity = addressEntity,
                documents = buyer.documents.map { DocumentEntity.fromDomain(it) }.toMutableList()
            ).apply {
                id = buyer.id
                createdAt = buyer.createdAt
                updatedAt = buyer.updatedAt
                
                addressEntity.buyer = this
                documents.forEach { it.buyer = this }
            }
            return entity
        }
    }

    fun toDomain(): Buyer {
        return Buyer(
            id = id,
            name = name,
            email = email,
            phone = phone,
            birthDate = birthDate,
            address = addressEntity.toDomain(),
            documents = documents.map { it.toDomain() },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
