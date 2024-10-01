package de.tum.cit.cs.benchmarkservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class GitHubClientConfiguration(
    @Value("\${github.repository.url}")
    private val url: String
) {

    @Bean
    fun gitHubClient(): WebClient {
        return WebClient.create(url)
    }
}
