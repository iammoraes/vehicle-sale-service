package com.vehiclemarketplace.infrastructure.storage

import com.vehiclemarketplace.infrastructure.storage.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Service
class S3StorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${app.aws.s3.bucket-name}") private val bucketName: String
) : StorageService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun upload(filename: String, content: ByteArray, contentType: String): FileInfo =
        withContext(Dispatchers.IO) {
            try {
                val key = sanitizeKey(filename)
                val fileId = UUID.randomUUID().toString()
                val finalKey = "$fileId/$key"

                val putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(finalKey)
                    .contentType(contentType)
                    .contentLength(content.size.toLong())
                    .metadata(mapOf(
                        "original-filename" to filename,
                        "upload-timestamp" to LocalDateTime.now().toString()
                    ))
                    .build()

                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content))

                val url = "https://$bucketName.s3.amazonaws.com/$finalKey"
                
                logger.info("Successfully uploaded file: $filename to S3 with key: $finalKey")

                com.vehiclemarketplace.infrastructure.storage.model.FileInfo(
                    id = fileId,
                    filename = filename,
                    url = url,
                    contentType = contentType,
                    size = content.size.toLong()
                )
            } catch (e: Exception) {
                logger.error("Failed to upload file: $filename", e)
                throw com.vehiclemarketplace.infrastructure.exception.StorageException("Failed to upload file: ${e.message}", e)
            }
        }

    override suspend fun delete(url: String) = withContext(Dispatchers.IO) {
        try {
            val key = extractKeyFromUrl(url)
            
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()

            s3Client.deleteObject(deleteObjectRequest)
            logger.info("Successfully deleted file with key: $key")
        } catch (e: Exception) {
            logger.error("Failed to delete file: $url", e)
            throw com.vehiclemarketplace.infrastructure.exception.StorageException("Failed to delete file: ${e.message}", e)
        }
    }

    override suspend fun generatePresignedUrl(filename: String, expirationMinutes: Int): String = 
        withContext(Dispatchers.IO) {
            try {
                val key = sanitizeKey(filename)
                
                val getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

                val presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes.toLong()))
                    .getObjectRequest(getObjectRequest)
                    .build()

                val presignedRequest = s3Presigner.presignGetObject(presignRequest)
                presignedRequest.url().toString()
            } catch (e: Exception) {
                logger.error("Failed to generate presigned URL for: $filename", e)
                throw com.vehiclemarketplace.infrastructure.exception.StorageException("Failed to generate presigned URL: ${e.message}", e)
            }
        }

    private fun sanitizeKey(filename: String): String {
        return filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun extractKeyFromUrl(url: String): String {
        return try {
            val urlObj = URL(url)
            urlObj.path.removePrefix("/")
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid S3 URL: $url", e)
        }
    }
}
