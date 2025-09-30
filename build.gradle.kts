import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.23"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    jacoco
    id("org.sonarqube") version "4.4.1.3373"
}

group = "com.vehiclemarketplace"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    compileOnly ("org.projectlombok:lombok")
    annotationProcessor ("org.projectlombok:lombok")

    implementation(platform("software.amazon.awssdk:bom:2.21.0"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:kms")
    implementation("software.amazon.awssdk:cognitoidentityprovider")
    implementation("software.amazon.awssdk:waf")
    implementation("software.amazon.awssdk:wafv2")
    implementation("software.amazon.awssdk:wafv2-logging")
    implementation("software.amazon.awssdk:wafv2-regex-pattern-set")
    implementation("software.amazon.awssdk:cloudwatch")
    implementation("software.amazon.awssdk:cloudwatchlogs")
    implementation("software.amazon.awssdk:cloudtrail")
    implementation("software.amazon.awssdk:guardduty")
    implementation("io.awspring.cloud:spring-cloud-aws-starter:3.0.2")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.0.2")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager:3.0.2")

    implementation("com.mercadopago:sdk-java:2.1.24")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/exception/**",
                    "**/model/**",
                    "**/dto/**"
                )
            }
        })
    )
}

sonar {
    properties {
        property("sonar.projectKey", "vehicle-marketplace")
        property("sonar.projectName", "Vehicle Marketplace")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.token", System.getenv("SONAR_TOKEN") ?: "")
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "${buildDir}/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.language", "kotlin")
        property("sonar.kotlin.detekt.reportPaths", "${buildDir}/reports/detekt/detekt.xml")
    }
}