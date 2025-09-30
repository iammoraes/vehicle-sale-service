package com.vehiclemarketplace.infrastructure.storage

import com.vehiclemarketplace.infrastructure.storage.model.FileInfo

interface StorageService {
    suspend fun upload(filename: String, content: ByteArray, contentType: String): FileInfo
    suspend fun delete(url: String)
    suspend fun generatePresignedUrl(filename: String, expirationMinutes: Int = 60): String
}
