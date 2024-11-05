# TUManyBenchmarks - Back-end Application
## About
This directory contains the TUManyBenchmarks backend-end application.
Its purpose is to provide front-end application data 
about instances and benchmark results from MongoDB.
Additionally, it parses information from AWS about EC2 prices and 
add it to the data from database.

## Technologies
The project was created using following technologies:
- Kotlin - modern JVM language
- Spring Boot - the most popular framework for microservices in JVM
- Spring Webflux - reactive programming and handling user requests
- Spring Data MongoDB Reactive - communication with MongoDB
- AWS Kotlin SDK - extracting EC2 prices from AWS
- Bucket4J - rate limiting API based on IP
- Caffeine Cache - caching data from MongoDB
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
Run command `java -jar webpage-backend-1.0.0.jar` with set proper environmental variables to run service.