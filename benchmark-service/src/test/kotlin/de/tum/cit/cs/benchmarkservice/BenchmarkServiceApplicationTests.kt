package de.tum.cit.cs.benchmarkservice

import org.junit.jupiter.api.Test
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ContextConfiguration(classes = [MongoTestContainerConfig::class])
class BenchmarkServiceApplicationTests {

    @Test
    fun contextLoads() {
    }

}
