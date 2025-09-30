package com.vehiclemarketplace.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.wafv2.Wafv2Client

/**
 * Configuração para AWS WAF (Web Application Firewall)
 * O WAF protege as APIs contra ataques comuns como SQL Injection, XSS, etc.
 */
@Configuration
class AwsWafConfig {

    @Value("\${aws.region}")
    private lateinit var region: String

    @Value("\${aws.credentials.access-key:}")
    private lateinit var accessKey: String

    @Value("\${aws.credentials.secret-key:}")
    private lateinit var secretKey: String

    @Value("\${aws.waf.web-acl-id:}")
    private lateinit var webAclId: String

    @Bean
    fun wafClient(): Wafv2Client {
        return if (accessKey.isNotBlank() && secretKey.isNotBlank()) {
            // Usar credenciais explícitas (para ambiente de desenvolvimento)
            val credentials = AwsBasicCredentials.create(accessKey, secretKey)
            Wafv2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()
        } else {
            // Usar cadeia de provedores padrão para ambiente de produção
            Wafv2Client.builder()
                .region(Region.of(region))
                .build()
        }
    }

    @Bean
    fun webAclId(): String = webAclId
}
