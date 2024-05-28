package de.tum.cit.cs.benchmarkservice

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container

@Configuration
@EnableReactiveMongoRepositories
class MongoTestContainerConfig {
    @Container
    final val mongoDBContainer: MongoDBContainer = MongoDBContainer("mongo:latest")
        .withExposedPorts(27017)

    init {
        mongoDBContainer.start()
        val mappedPort = mongoDBContainer.getMappedPort(27017)
        System.setProperty("mongodb.container.port", mappedPort.toString())
    }
}
