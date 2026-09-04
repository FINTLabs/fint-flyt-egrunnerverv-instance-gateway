import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath(platform("com.fasterxml.jackson:jackson-bom:2.22.2"))
        constraints {
            classpath("org.apache.httpcomponents.client5:httpclient5:5.6.4")
            classpath("org.apache.httpcomponents.core5:httpcore5:5.4.3")
            classpath("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
            classpath("org.apache.commons:commons-lang3:3.20.0")
        }
    }
}

plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.github.ben-manes.versions") version "0.61.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    jacoco
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.spring") version "2.4.10"
    kotlin("plugin.jpa") version "2.4.10"
}

group = "no.novari"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.fintlabs.no/releases")
    }
    mavenLocal()
}

val fintModelResourceVersion = "1.0.1"
val fintResourceModelVersion = "4.1.0"

extra["commons-lang3.version"] = "3.20.0"
extra["httpclient5.version"] = "5.6.3"
extra["httpcore5.version"] = "5.4.3"
extra["jackson-bom.version"] = "2.22.2"
extra["log4j2.version"] = "2.26.1"
extra["netty.version"] = "4.2.17.Final"
extra["postgresql.version"] = "42.7.12"
extra["tomcat.version"] = "10.1.59"

dependencies {
    constraints {
        implementation("at.yawk.lz4:lz4-java:1.11.2") {
            because("Fixes CVE-2026-59949 in the kafka-clients transitive dependency")
        }
        testImplementation("org.apache.commons:commons-compress:1.28.0") {
            because("Fixes CVE-2024-25710 and CVE-2024-26308 in the Testcontainers transitive dependency")
        }
    }

    implementation(platform("tools.jackson:jackson-bom:3.2.2"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("no.novari:flyt-web-instance-gateway:3.0.0")
    implementation("no.novari:flyt-cache:3.0.0")

    implementation("no.novari:fint-model-resource:$fintModelResourceVersion")
    implementation("no.novari:fint-arkiv-resource-model-java:$fintResourceModelVersion")
    implementation("no.novari:fint-administrasjon-resource-model-java:$fintResourceModelVersion")

    // For compatibility with the fint models pulled in above
    implementation("javax.validation:validation-api:2.0.1.Final")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

ktlint {
    version.set("1.8.0")
}

tasks.named("check") {
    dependsOn("ktlintCheck")
    dependsOn("jacocoTestReport")
}
