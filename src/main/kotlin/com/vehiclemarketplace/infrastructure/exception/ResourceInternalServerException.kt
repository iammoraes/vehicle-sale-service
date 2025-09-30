package com.vehiclemarketplace.infrastructure.exception

class ResourceInternalServerException(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)