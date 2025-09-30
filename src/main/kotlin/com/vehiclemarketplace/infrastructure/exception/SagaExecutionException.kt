package com.vehiclemarketplace.infrastructure.exception

class SagaExecutionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
