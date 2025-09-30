package com.vehiclemarketplace.domain.model.sale

import java.math.BigDecimal

data class SalesSummary(
    val totalSales: Int,
    val totalRevenue: BigDecimal,
    val totalVehiclesSold: Int,
    val averageSaleValue: BigDecimal,
    val salesByStatus: Map<SaleStatus, Int>,
    val salesByPaymentMethod: Map<PaymentMethod, Int>,
    val salesByVehicleBrand: Map<String, Int>,
    val salesByMonth: Map<String, Int>
)