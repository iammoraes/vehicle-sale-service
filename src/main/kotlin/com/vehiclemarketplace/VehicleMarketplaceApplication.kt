package com.vehiclemarketplace

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "com.vehiclemarketplace.application",
        "com.vehiclemarketplace.domain",
        "com.vehiclemarketplace.infrastructure"
    ]
)
@EnableScheduling
class VehicleMarketplaceApplication

fun main(args: Array<String>) {
    runApplication<VehicleMarketplaceApplication>(*args)
}
