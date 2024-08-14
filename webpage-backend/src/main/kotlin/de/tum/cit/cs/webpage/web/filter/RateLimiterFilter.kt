package de.tum.cit.cs.webpage.web.filter

import de.tum.cit.cs.webpage.common.Headers.X_API_KEY_HEADER
import de.tum.cit.cs.webpage.service.ApiKeyService
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Order(2)
@Component
class RateLimiterFilter(private val apiKeyService: ApiKeyService) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (shouldFilter(exchange)) {
            val apiKey = exchange.request.headers.getFirst(X_API_KEY_HEADER)
            val isAccessAllowed = apiKey?.let(apiKeyService::isAccessAllowed)
            return if (isAccessAllowed == null) {
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                exchange.response.headers.contentType = MediaType.APPLICATION_JSON
                val errorMessage = """{"error": "No valid API key found"}"""
                val buffer = exchange.response.bufferFactory().wrap(errorMessage.toByteArray())
                exchange.response.writeWith(Mono.just(buffer))
            } else if (isAccessAllowed) {
                chain.filter(exchange)
            } else {
                exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                exchange.response.headers.contentType = MediaType.APPLICATION_JSON
                val errorMessage = """{"error": "Too many requests in one minute"}"""
                val buffer = exchange.response.bufferFactory().wrap(errorMessage.toByteArray())
                exchange.response.writeWith(Mono.just(buffer))
            }
        }
        return chain.filter(exchange)
    }

    private fun shouldFilter(exchange: ServerWebExchange): Boolean {
        val path = exchange.request.uri.path
        return !path.startsWith("/api/key")
    }
}
