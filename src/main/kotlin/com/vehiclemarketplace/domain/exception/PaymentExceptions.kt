package com.vehiclemarketplace.domain.exception

open class PaymentException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class PaymentNotFoundException(
    paymentId: String,
    cause: Throwable? = null
) : PaymentException("Payment not found with id: $paymentId", cause)

class PaymentCreationException(
    message: String,
    cause: Throwable? = null
) : PaymentException("Failed to create payment: $message", cause)

class PaymentGatewayException(
    gateway: String,
    message: String,
    cause: Throwable? = null
) : PaymentException("Payment gateway error [$gateway]: $message", cause)
