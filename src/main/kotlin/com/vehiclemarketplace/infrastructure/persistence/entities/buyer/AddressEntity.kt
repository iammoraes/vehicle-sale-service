package com.vehiclemarketplace.infrastructure.persistence.entities.buyer

import com.fasterxml.jackson.annotation.JsonIgnore
import com.vehiclemarketplace.domain.model.buyer.Address
import com.vehiclemarketplace.infrastructure.persistence.entities.BaseEntity
import jakarta.persistence.*
import lombok.ToString

/**
 * Entity class for Address used in persistence layer
 */
@Entity
@Table(name = "buyer_addresses")
class AddressEntity(
    @Column(nullable = false)
    var street: String,

    @Column(nullable = false)
    var number: String,

    @Column
    var complement: String? = null,

    @Column(nullable = false)
    var neighborhood: String,

    @Column(nullable = false)
    var city: String,

    @Column(nullable = false)
    var state: String,

    @Column(name = "postal_code", nullable = false)
    var postalCode: String,

    @Column(nullable = false)
    var country: String,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_buyer", nullable = false)
    @ToString.Exclude
    var buyer: BuyerEntity? = null
) : BaseEntity() {

    override fun toString(): String {
        return "AddressEntity(id=$id, street=$street, number=$number, complement=$complement," +
                " neighborhood=$neighborhood, city=$city, state=$state, postalCode=$postalCode, country=$country)"
    }

    companion object {
        fun fromDomain(address: Address): AddressEntity {
            return AddressEntity(
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

    fun toDomain(): Address {
        return Address(
            street = this.street,
            number = this.number,
            complement = this.complement,
            neighborhood = this.neighborhood,
            city = this.city,
            state = this.state,
            postalCode = this.postalCode,
            country = this.country
        )
    }
}
