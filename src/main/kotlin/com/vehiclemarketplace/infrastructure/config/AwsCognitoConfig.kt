package com.vehiclemarketplace.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.vehiclemarketplace.infrastructure.security.AwsSecretsManagerService
import javax.annotation.PostConstruct

@Configuration
class AwsCognitoConfig {

    @Value("\${aws.cognito.secret-name:cognito-config}")
    private lateinit var cognitoSecretName: String
    
    @Value("\${aws.region}")
    private lateinit var region: String
    
    private lateinit var userPoolId: String
    private lateinit var clientId: String
    private lateinit var clientSecret: String

    @PostConstruct
    fun init(secretsManagerService: AwsSecretsManagerService) {
        this.userPoolId = secretsManagerService.getSecretJsonValue(cognitoSecretName, "user_pool_id") 
            ?: throw IllegalStateException("Failed to load Cognito user_pool_id from Secrets Manager")
        
        this.clientId = secretsManagerService.getSecretJsonValue(cognitoSecretName, "client_id")
            ?: throw IllegalStateException("Failed to load Cognito client_id from Secrets Manager")
        
        this.clientSecret = secretsManagerService.getSecretJsonValue(cognitoSecretName, "client_secret")
            ?: throw IllegalStateException("Failed to load Cognito client_secret from Secrets Manager")
    }

    @Bean
    fun cognitoClient(awsCredentialsProvider: AwsCredentialsProvider): CognitoIdentityProviderClient {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(region))
            .build()
    }

    @Bean
    fun userPoolId(): String {
        return userPoolId
    }

    @Bean
    fun clientId(): String {
        return clientId
    }

    @Bean
    fun clientSecret(): String {
        return clientSecret
    }
}
