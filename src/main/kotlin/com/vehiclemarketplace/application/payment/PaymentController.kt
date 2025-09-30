package com.vehiclemarketplace.application.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.vehiclemarketplace.application.payment.dto.NotificationRequest
import com.vehiclemarketplace.domain.service.payment.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Payments", description = "Payment processing APIs")
class PaymentController(
    private val paymentService: PaymentService,
    private val objectMapper: ObjectMapper
) {

    @PostMapping("/webhook")
    @Operation(summary = "Process payment notifications from Mercado Pago")
    suspend fun webhook(@RequestBody rawPayload: String): ResponseEntity<Map<String, String>> {
        try {
            val notification = objectMapper.readValue(rawPayload, NotificationRequest::class.java)
            val result = paymentService.processPaymentNotification(notification)
            return ResponseEntity.ok(result)
        } catch (e: Exception) {
            return ResponseEntity.ok(mapOf("status" to "error", "message" to (e.message ?: "Unknown error")))
        }
    }
}