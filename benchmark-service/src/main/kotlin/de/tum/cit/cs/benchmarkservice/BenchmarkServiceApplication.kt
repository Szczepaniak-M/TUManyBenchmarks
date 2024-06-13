package de.tum.cit.cs.benchmarkservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
@EnableReactiveMongoRepositories
class BenchmarkServiceApplication

fun main(args: Array<String>) {
    runApplication<BenchmarkServiceApplication>(*args)
}
