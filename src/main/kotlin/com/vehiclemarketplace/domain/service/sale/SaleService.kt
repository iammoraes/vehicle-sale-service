package com.vehiclemarketplace.domain.service.sale

import com.vehiclemarketplace.application.sale.dto.SaleRequest
import com.vehiclemarketplace.application.sale.dto.SaleResponse
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse
import java.util.UUID

interface SaleService {
    suspend fun initiateSale(request: SaleRequest): SaleResponse
    suspend fun cancelSale(saleId: UUID, reason: String): SaleResponse?
    suspend fun confirmDelivery(saleId: UUID): SaleResponse?
    suspend fun updateSale(transactionId: String, paymentGatewayResponse: PaymentGatewayResponse): Map<String, String>

}