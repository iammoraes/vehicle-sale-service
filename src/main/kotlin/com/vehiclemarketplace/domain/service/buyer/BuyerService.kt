package com.vehiclemarketplace.domain.service.buyer

import com.vehiclemarketplace.application.buyer.dto.BuyerDto
import com.vehiclemarketplace.domain.model.buyer.BuyerId

interface BuyerService {
    suspend fun createBuyer(buyerDto: BuyerDto): BuyerDto
    suspend fun getBuyerById(id: BuyerId): BuyerDto
}