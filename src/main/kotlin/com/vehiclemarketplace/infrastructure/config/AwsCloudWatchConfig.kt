package com.vehiclemarketplace.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient

/**
 * Configuração para AWS CloudWatch
 * Permite monitoramento centralizado e logs para a aplicação
 */
@Configuration
class AwsCloudWatchConfig {

    @Value("\${aws.region}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key:}")
    private lateinit var accessKey: String

    @Value("\${aws.credentials.secret-key:}")
    private lateinit var secretKey: String

    @Value("\${aws.cloudwatch.log-group:}")
    private lateinit var logGroup: String

    @Bean
    fun cloudWatchClient(): CloudWatchClient {
        return if (accessKey.isNotBlank() && secretKey.isNotBlank()) {
            // Usar credenciais explícitas (para ambiente de desenvolvimento)
            val credentials = AwsBasicCredentials.create(accessKey, secretKey)
            CloudWatchClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()
        } else {
            // Usar cadeia de provedores padrão para ambiente de produção
            CloudWatchClient.builder()
                .region(Region.of(region))
                .build()
        }
    }

    @Bean
    fun cloudWatchLogsClient(): CloudWatchLogsClient {
        return if (accessKey.isNotBlank() && secretKey.isNotBlank()) {
            // Usar credenciais explícitas (para ambiente de desenvolvimento)
            val credentials = AwsBasicCredentials.create(accessKey, secretKey)
            CloudWatchLogsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()
        } else {
            // Usar cadeia de provedores padrão para ambiente de produção
            CloudWatchLogsClient.builder()
                .region(Region.of(region))
                .build()
        }
    }

    @Bean
    fun logGroup(): String = logGroup
}
