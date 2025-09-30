package com.vehiclemarketplace.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

/**
 * Configuração para AWS Secrets Manager
 * Permite armazenar e recuperar credenciais e outros segredos de forma segura
 */
@Configuration
class AwsSecretsManagerConfig {

    @Value("\${aws.region}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key:}")
    private lateinit var accessKey: String

    @Value("\${aws.credentials.secret-key:}")
    private lateinit var secretKey: String

    @Bean
    fun secretsManagerClient(): SecretsManagerClient {
        return if (accessKey.isNotBlank() && secretKey.isNotBlank()) {
            // Usar credenciais explícitas (para ambiente de desenvolvimento)
            val credentials = AwsBasicCredentials.create(accessKey, secretKey)
            SecretsManagerClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()
        } else {
            // Usar cadeia de provedores padrão para ambiente de produção
            SecretsManagerClient.builder()
                .region(Region.of(region))
                .build()
        }
    }
}
