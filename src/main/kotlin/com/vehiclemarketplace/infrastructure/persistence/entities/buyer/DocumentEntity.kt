package com.vehiclemarketplace.infrastructure.persistence.entities.buyer

import com.fasterxml.jackson.annotation.JsonIgnore
import com.vehiclemarketplace.domain.model.buyer.Document
import com.vehiclemarketplace.domain.model.buyer.DocumentType
import com.vehiclemarketplace.infrastructure.persistence.entities.BaseEntity
import jakarta.persistence.*
import lombok.ToString

@Entity
@Table(name = "buyer_documents")
class DocumentEntity(
    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: DocumentType,
    
    @Column(nullable = false)
    var number: String,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_buyer", nullable = false)
    @ToString.Exclude
    var buyer: BuyerEntity? = null
) : BaseEntity() {

    override fun toString(): String {
        return "DocumentEntity(id=$id, type=$type, number=$number)"
    }
    
    companion object {
        fun fromDomain(document: Document): DocumentEntity {
            return DocumentEntity(
                type = document.type,
                number = document.number
            )
        }
    }
    
    fun toDomain(): Document {
        return Document(
            type = type,
            number = number
        )
    }
}
