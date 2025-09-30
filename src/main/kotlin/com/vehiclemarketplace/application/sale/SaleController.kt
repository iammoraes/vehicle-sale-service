package com.vehiclemarketplace.application.sale

import com.vehiclemarketplace.application.sale.dto.CancelSaleRequest
import com.vehiclemarketplace.application.sale.dto.SaleRequest
import com.vehiclemarketplace.application.sale.dto.SaleResponse
import com.vehiclemarketplace.domain.service.sale.SaleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/sales")
@Tag(name = "Sales", description = "Vehicle sale management APIs")
class SaleController(
    private val saleService: SaleService
) {

    @PostMapping
    @Operation(summary = "Init a vehicle sale")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun initiateSale(@RequestBody request: SaleRequest): ResponseEntity<SaleResponse> {
        val sale = saleService.initiateSale(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(sale)
    }

    @PostMapping("/{saleId}/cancel")
    @Operation(summary = "Cancel a sale")
    suspend fun cancelSale(
        @PathVariable saleId: UUID,
        @RequestBody request: CancelSaleRequest
    ): ResponseEntity<SaleResponse> {
        return saleService.cancelSale(saleId, request.reason)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping("/{saleId}/confirm-delivery")
    @Operation(summary = "Confirmation of vehicle delivery")
    suspend fun confirmDelivery(
        @PathVariable saleId: UUID): ResponseEntity<SaleResponse> {
        return saleService.confirmDelivery(saleId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
