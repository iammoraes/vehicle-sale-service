package com.vehiclemarketplace.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import com.vehiclemarketplace.infrastructure.aws.AwsSecretsManagerService
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
class AwsConfig(
    @Value("\${app.aws.region}") private val region: String,
    @Value("\${aws.credentials.secret-name:aws-credentials}") private val credentialsSecretName: String
) {
    
    private lateinit var accessKey: String
    private lateinit var secretKey: String
    
    @Bean(name = ["awsCredentialsProvider"])
    fun awsCredentialsProvider(secretsManagerService: AwsSecretsManagerService): AwsCredentialsProvider {
        val credentials = secretsManagerService.getSecretJsonValue(credentialsSecretName, "access_key")?.let { accessKey ->
            secretsManagerService.getSecretJsonValue(credentialsSecretName, "secret_key")?.let { secretKey ->
                this.accessKey = accessKey
                this.secretKey = secretKey
                AwsBasicCredentials.create(accessKey, secretKey)
            }
        } ?: throw IllegalStateException("Failed to load AWS credentials from Secrets Manager")
        
        return StaticCredentialsProvider.create(credentials)
    }

    @Bean(name = ["awsRegion"])
    fun awsRegion(): Region {
        return Region.of(region)
    }

    @Bean
    fun s3Client(@Value("\${app.aws.region}") region: String, awsCredentialsProvider: AwsCredentialsProvider): S3Client {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(awsCredentialsProvider)
            .build()
    }

    @Bean
    fun s3Presigner(@Value("\${app.aws.region}") region: String, awsCredentialsProvider: AwsCredentialsProvider): S3Presigner {
        return S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(awsCredentialsProvider)
            .build()
    }

    @Bean
    fun secretsManagerClient(@Value("\${app.aws.region}") region: String, awsCredentialsProvider: AwsCredentialsProvider): SecretsManagerClient {
        return SecretsManagerClient.builder()
            .region(Region.of(region))
            .credentialsProvider(awsCredentialsProvider)
            .build()
    }

    @Bean
    fun sqsClient(@Value("\${app.aws.region}") region: String, awsCredentialsProvider: AwsCredentialsProvider): SqsClient {
        return SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(awsCredentialsProvider)
            .build()
    }
}
