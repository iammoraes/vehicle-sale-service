package com.vehiclemarketplace.application.buyer

import com.vehiclemarketplace.application.buyer.dto.BuyerDto
import com.vehiclemarketplace.domain.service.buyer.BuyerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/buyers")
@Tag(name = "Buyers", description = "Buyer management APIs")
class BuyerController(
    private val buyerService: BuyerService
) {

    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Create a new buyer")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createBuyer(@RequestBody request: BuyerDto): ResponseEntity<BuyerDto> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(buyerService.createBuyer(request))
    }
}
