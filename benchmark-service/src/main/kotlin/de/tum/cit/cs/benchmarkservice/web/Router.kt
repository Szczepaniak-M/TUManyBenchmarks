package de.tum.cit.cs.benchmarkservice.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class Router {
    @Bean
    fun route(restController: RestController) =
        coRouter {
            accept(MediaType.APPLICATION_JSON).nest {
                DELETE("/api/service").invoke(restController::stopBenchmarkExecution)
            }
        }
}