import dev.detekt.gradle.Detekt
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "3.5.16"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.6"
}

group = "com.whsanha55"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // io.spring.dependency-management 플러그인 대신 BOM platform으로 버전을 관리한다.
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.16"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
        javaParameters = true
    }
}

// 로컬에는 JDK 25만 있으므로 toolchain은 쓰지 않고 바이트코드 타깃만 21로 맞춘다.
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // SecurityProperties는 빈 토큰이면 기동에 실패하므로 테스트 실행에 값을 주입한다.
    environment("FEEDFORGE_ACCESS_TOKEN", "test-access-token")
}

// Entity의 protected set과 지연 로딩 프록시를 위해 open으로 만든다.
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

// detekt 1.23.x는 내장 Kotlin 컴파일러가 JDK 25를 인식하지 못해 기동 자체가 실패한다.
// JDK 25 지원과 Gradle 9 호환을 위해 2.0.0-alpha를 쓴다.
tasks.withType<Detekt>().configureEach {
    jvmTarget = "21"
}
