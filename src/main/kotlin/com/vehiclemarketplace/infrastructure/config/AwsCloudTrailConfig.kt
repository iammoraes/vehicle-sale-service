package com.vehiclemarketplace.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient

/**
 * Configuração para AWS CloudTrail
 * Permite auditoria e rastreamento de atividades na infraestrutura AWS
 */
@Configuration
class AwsCloudTrailConfig {

    @Value("\${aws.region}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key:}")
    private lateinit var accessKey: String

    @Value("\${aws.credentials.secret-key:}")
    private lateinit var secretKey: String

    @Value("\${aws.cloudtrail.trail-name:}")
    private lateinit var trailName: String

    @Bean
    fun cloudTrailClient(): CloudTrailClient {
        return if (accessKey.isNotBlank() && secretKey.isNotBlank()) {
            // Usar credenciais explícitas (para ambiente de desenvolvimento)
            val credentials = AwsBasicCredentials.create(accessKey, secretKey)
            CloudTrailClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()
        } else {
            // Usar cadeia de provedores padrão para ambiente de produção
            CloudTrailClient.builder()
                .region(Region.of(region))
                .build()
        }
    }

    @Bean
    fun trailName(): String = trailName
}
