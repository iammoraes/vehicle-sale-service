package com.vehiclemarketplace.domain.model.sale

enum class SaleEventType {
    SALE_CREATED,
    PAYMENT_INITIATED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    PAYMENT_PENDING,
    VEHICLE_RESERVED,
    VEHICLE_DELIVERED,
    SALE_COMPLETED,
    SALE_CANCELLED
}
