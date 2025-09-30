package com.vehiclemarketplace.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${server.port:8080}") private val serverPort: Int,
    @Value("\${server.servlet.context-path:/api}") private val contextPath: String
) {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Vehicle Marketplace API")
                    .description("""
                        ## Vehicle Marketplace Microservice
                        
                        A comprehensive vehicle resale platform built with Spring Boot 3.2.5, Kotlin, and following Clean Architecture principles.
                        
                        ### Key Features
                        - **Vehicle Management**: Complete CRUD operations for vehicle listings
                        - **Purchase Process**: End-to-end vehicle purchase workflow with SAGA pattern
                        - **Security**: JWT-based authentication with role-based access control
                        - **LGPD Compliance**: Data protection, audit trails, and consent management
                        - **File Storage**: AWS S3 integration for vehicle images
                        - **Payment Processing**: Multiple payment methods support
                        
                        ### Architecture
                        - **Hexagonal Architecture**: Clean separation of concerns
                        - **SAGA Pattern**: Distributed transaction management
                        - **Event Sourcing**: Complete audit trail for all operations
                        - **CQRS**: Command Query Responsibility Segregation
                        
                        ### Security & Compliance
                        - JWT Authentication with role-based authorization
                        - Data encryption for sensitive information
                        - Comprehensive audit logging
                        - LGPD compliance with data anonymization
                        - Input validation and sanitization
                        
                        ### Roles
                        - **BUYER**: Can browse vehicles, make purchases, manage profile
                        - **SELLER**: Can manage vehicle listings, view sales
                        - **ADMIN**: Full system access, user management, reports
                    """.trimIndent())
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Vehicle Marketplace Team")
                            .email("support@vehiclemarketplace.com")
                            .url("https://vehiclemarketplace.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort$contextPath")
                        .description("Local Development Server"),
                    Server()
                        .url("https://api.vehiclemarketplace.com")
                        .description("Production Server"),
                    Server()
                        .url("https://staging-api.vehiclemarketplace.com")
                        .description("Staging Server")
                )
            )
            .addSecurityItem(
                SecurityRequirement().addList("Bearer Authentication")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "Bearer Authentication",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token for API authentication. Format: Bearer {token}")
                    )
            )
    }
}
