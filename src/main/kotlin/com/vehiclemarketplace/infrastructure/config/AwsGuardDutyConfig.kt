package com.vehiclemarketplace.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.guardduty.GuardDutyClient

/**
 * Configuração para AWS GuardDuty
 * Serviço de detecção de ameaças que monitora continuamente comportamentos maliciosos
 */
@Configuration
class AwsGuardDutyConfig {

    @Value("\${aws.region}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key:}")
    private lateinit var accessKey: String

    @Value("\${aws.credentials.secret-key:}")
    private lateinit var secretKey: String

    @Value("\${aws.guardduty.detector-id:}")
    private lateinit var detectorId: String

    @Bean
    fun guardDutyClient(): GuardDutyClient {
        return if (accessKey.isNotBlank() && secretKey.isNotBlank()) {
            // Usar credenciais explícitas (para ambiente de desenvolvimento)
            val credentials = AwsBasicCredentials.create(accessKey, secretKey)
            GuardDutyClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()
        } else {
            // Usar cadeia de provedores padrão para ambiente de produção
            GuardDutyClient.builder()
                .region(Region.of(region))
                .build()
        }
    }

    @Bean
    fun detectorId(): String = detectorId
}
