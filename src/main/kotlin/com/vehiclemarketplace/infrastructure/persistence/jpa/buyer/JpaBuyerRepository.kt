package com.vehiclemarketplace.infrastructure.persistence.jpa.buyer

import com.vehiclemarketplace.infrastructure.persistence.entities.buyer.BuyerEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaBuyerRepository : JpaRepository<BuyerEntity, UUID> {

    fun existsByEmail(email: String): Boolean
    
    @Query("SELECT b FROM BuyerEntity b LEFT JOIN FETCH b.documents LEFT JOIN FETCH b.addressEntity WHERE b.id = :id")
    fun findByIdWithDocuments(@Param("id") id: UUID): Optional<BuyerEntity>
}
