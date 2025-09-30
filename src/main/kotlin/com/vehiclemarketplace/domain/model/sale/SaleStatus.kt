package com.vehiclemarketplace.domain.model.sale

enum class SaleStatus {
    PENDING_PAYMENT,
    PAYMENT_APPROVED,
    PAYMENT_DECLINED,
    PAYMENT_PENDING,
    VEHICLE_RESERVED,
    VEHICLE_DELIVERED,
    COMPLETED,
    CANCELLED
}
