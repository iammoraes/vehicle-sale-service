package com.vehiclemarketplace.domain.repositories.buyer

import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.buyer.BuyerId

interface BuyerRepository {
    suspend fun findById(id: BuyerId): Buyer
    suspend fun save(buyer: Buyer): Buyer
    suspend fun existsByEmail(email: String): Boolean
}
