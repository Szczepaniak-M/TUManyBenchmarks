package de.tum.cit.cs.webpage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
@EnableReactiveMongoRepositories
class WebpageBackendApplication

fun main(args: Array<String>) {
    runApplication<WebpageBackendApplication>(*args)
}
