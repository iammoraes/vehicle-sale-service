package com.vehiclemarketplace.application.buyer

import com.vehiclemarketplace.application.buyer.dto.BuyerDto
import com.vehiclemarketplace.application.buyer.dto.BuyerDto.Companion.fromDomain
import com.vehiclemarketplace.domain.model.buyer.BuyerId
import com.vehiclemarketplace.domain.repositories.buyer.BuyerRepository
import com.vehiclemarketplace.domain.service.buyer.BuyerService
import org.springframework.stereotype.Service

@Service
class BuyerServiceImpl(
    private val buyerRepository: BuyerRepository
) : BuyerService {

    override suspend fun createBuyer(buyerDto: BuyerDto): BuyerDto {
        if (buyerDto.email.isNotEmpty() && buyerRepository.existsByEmail(buyerDto.email)) {
            throw IllegalArgumentException("Buyer with email ${buyerDto.email} already exists")
        }

        val buyer = buyerDto.toDomain()
        val savedBuyer = buyerRepository.save(buyer)
        
        return fromDomain(savedBuyer)
    }
    
    override suspend fun getBuyerById(id: BuyerId): BuyerDto {
        return fromDomain(buyerRepository.findById(id))
    }
}
