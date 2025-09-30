package com.vehiclemarketplace.infrastructure.exception.handler

import com.vehiclemarketplace.infrastructure.exception.*
import com.vehiclemarketplace.infrastructure.exception.model.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(VehicleNotFoundException::class)
    fun handleVehicleNotFound(ex: VehicleNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Vehicle not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Vehicle not found", request))
    }

    @ExceptionHandler(BuyerNotFoundException::class)
    fun handleBuyerNotFound(ex: BuyerNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Buyer not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Buyer not found", request))
    }

    @ExceptionHandler(SaleNotFoundException::class)
    fun handleSaleNotFound(ex: SaleNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Sale not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "Sale not found", request))
    }

    @ExceptionHandler(VehicleNotAvailableException::class)
    fun handleVehicleNotAvailable(ex: VehicleNotAvailableException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Vehicle not available: ${ex.message}")
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(createErrorResponse(HttpStatus.CONFLICT, ex.message ?: "Vehicle not available", request))
    }

    @ExceptionHandler(PaymentProcessingException::class)
    fun handlePaymentProcessing(ex: PaymentProcessingException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Payment processing error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(createErrorResponse(HttpStatus.PAYMENT_REQUIRED, ex.message ?: "Payment processing failed", request))
    }

    @ExceptionHandler(SagaExecutionException::class)
    fun handleSagaExecution(ex: SagaExecutionException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("SAGA execution error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction processing failed", request))
    }

    @ExceptionHandler(DataProtectionException::class)
    fun handleDataProtection(ex: DataProtectionException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Data protection error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Data protection error", request))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.map { error ->
            when (error) {
                is FieldError -> "${error.field}: ${error.defaultMessage}"
                else -> error.defaultMessage ?: "Validation error"
            }
        }

        logger.warn("Validation errors: $errors")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                errors
            ))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid argument: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid argument", request))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: ${ex.message}")
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(createErrorResponse(HttpStatus.FORBIDDEN, "Access denied", request))
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Bad credentials: ${ex.message}")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", request))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request))
    }

    private fun createErrorResponse(
        status: HttpStatus,
        message: String,
        request: WebRequest,
        details: List<String>? = null
    ): ErrorResponse {
        return ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.getDescription(false).removePrefix("uri="),
            details = details
        )
    }
}
