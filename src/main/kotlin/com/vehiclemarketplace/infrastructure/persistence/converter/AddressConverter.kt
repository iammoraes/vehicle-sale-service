package com.vehiclemarketplace.infrastructure.persistence.converter

import com.vehiclemarketplace.domain.model.buyer.Address
import jakarta.persistence.Converter

@Converter(autoApply = true)
class AddressConverter : JsonbConverter<Address>() {
    override fun getTypeClass(): Class<Address> = Address::class.java
}
