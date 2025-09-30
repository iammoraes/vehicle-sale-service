package com.vehiclemarketplace.application.payment.dto

data class NotificationRequest(
    val type: String,
    val data: DataRequest
)

data class DataRequest(
    val id: String
)