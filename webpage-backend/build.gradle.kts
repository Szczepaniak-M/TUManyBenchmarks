plugins {
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("jvm") version "2.0.20"
	kotlin("plugin.spring") version "2.0.20"
}

group = "de.tum.cit.cs"
version = "1.0.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("aws.sdk.kotlin:pricing:1.3.67") {
		exclude("com.squareup.okhttp3:okhttp")
	}
	implementation("aws.sdk.kotlin:ec2-jvm:1.3.67") {
		exclude("com.squareup.okhttp3:okhttp")
	}
	implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
	implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
	implementation("com.github.ben-manes.caffeine:caffeine")
	implementation("com.bucket4j:bucket4j-core:8.10.1")

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "mockito-core")
	}
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mongodb")
	testImplementation("com.ninja-squad:springmockk:4.0.2")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
