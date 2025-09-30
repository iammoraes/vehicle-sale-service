package com.vehiclemarketplace.infrastructure.config

import com.mercadopago.MercadoPagoConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import javax.annotation.PostConstruct

@Configuration
@PropertySource("classpath:application.properties")
class MercadoPagoConfiguration {

    @Value("\${mercadopago.access.token:#{null}}")
    private val accessToken: String? = null

    @PostConstruct
    fun init() {
        if (accessToken.isNullOrBlank()) {
            throw IllegalStateException("MercadoPago access token is not configured. Set the mercadopago.access.token property.")
        }
        
        MercadoPagoConfig.setAccessToken(accessToken)
    }
}
