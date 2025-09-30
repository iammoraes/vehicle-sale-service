package com.vehiclemarketplace.infrastructure.persistence.repositories.buyer

import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.buyer.BuyerId
import com.vehiclemarketplace.domain.repositories.buyer.BuyerRepository
import com.vehiclemarketplace.infrastructure.exception.BuyerNotFoundException
import com.vehiclemarketplace.infrastructure.persistence.entities.buyer.BuyerEntity
import com.vehiclemarketplace.infrastructure.persistence.jpa.buyer.JpaBuyerRepository
import com.vehiclemarketplace.infrastructure.security.SensitiveDataProtectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository

@Repository
class BuyerRepositoryImpl(
    private val jpaBuyerRepository: JpaBuyerRepository,
    private val sensitiveDataProtectionService: SensitiveDataProtectionService
) : BuyerRepository {

    override suspend fun findById(id: BuyerId): Buyer = withContext(Dispatchers.IO) {
        val buyerEntity = jpaBuyerRepository.findByIdWithDocuments(id)
            .orElseThrow { throw BuyerNotFoundException("Buyer not found: $id") }
        sensitiveDataProtectionService.revealBuyerData(buyerEntity)
        buyerEntity.toDomain()
    }

    override suspend fun save(buyer: Buyer): Buyer = withContext(Dispatchers.IO) {
        val entity = BuyerEntity.fromDomain(buyer)
        sensitiveDataProtectionService.protectBuyerData(entity)

        val savedEntity = jpaBuyerRepository.save(entity)
        sensitiveDataProtectionService.revealBuyerData(savedEntity)
        savedEntity.toDomain()
    }

    override suspend fun existsByEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        jpaBuyerRepository.existsByEmail(email)
    }
}
