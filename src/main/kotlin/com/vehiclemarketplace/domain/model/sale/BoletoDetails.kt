package com.vehiclemarketplace.domain.model.sale

import java.time.LocalDateTime

data class BoletoDetails(
    val barcode: String,
    val dueDate: LocalDateTime?,
    val pdfUrl: String
) : PaymentDetails
