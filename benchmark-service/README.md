# TUManyBenchmarks - Benchmark Service
## About
This directory contains the TUManyBenchmarks benchmark service.
Its purpose is to provide run benchmarks uploaded by users to the repository.
Additionally, it downloads data about EC2 instances from AWS and save to MongoDB.
This service also create view used by the backend application.

## Technologies
The project was created using following technologies:
- Kotlin - modern JVM language
- Spring Boot - the most popular framework for microservices in JVM
- Spring Webflux - reactive programming and handling user requests
- Spring Data MongoDB Reactive - communication with MongoDB
- AWS Kotlin SDK - extracting EC2 information form AWS and infrastructure setup for benchmarks
- Apache Mina SSHD - SSH client
- Testcontainers - run MongoDB Docker container for integration tests
- Mockk - mocking in Kotlin

## Basic commands
### Development server
Run `./gradlew bootRun` for a dev server. Navigate to `http://localhost:8080/`.

### Build
Run `./gradlew build` to build the project. The build artifacts will be stored in the `build/libs` directory.

### Running unit tests
Run `./gradlew test` to execute the unit tests.

### Run
Run command `java -jar benchmark-service-1.0.0.jar` with set proper environmental variables to run service.

## Requirements:
To run the service, the machine requires:
- Java 21
- AmazonEC2FullAccess AMI Policy
- public IPv4 (to communicate with MongoDB Atlas) and IPv6 addresses (to communicate with other EC2)