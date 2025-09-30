package com.vehiclemarketplace.infrastructure.storage.model

import java.time.LocalDateTime

data class FileInfo(
    val id: String,
    val filename: String,
    val url: String,
    val contentType: String,
    val size: Long,
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)