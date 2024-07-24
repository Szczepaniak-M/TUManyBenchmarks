package de.tum.cit.cs.webpage.web

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
                GET("/api/key").invoke(restController::getApiKey)
                GET("/api/instance").invoke(restController::listInstances)
                GET("/api/instance/{instanceType}").invoke(restController::getInstance)
                POST("/api/explorer").invoke(restController::getExplorerResponse)
            }
        }
}
