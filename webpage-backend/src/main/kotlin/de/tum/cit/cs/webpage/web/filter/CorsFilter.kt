package de.tum.cit.cs.webpage.web.filter

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.cors.reactive.CorsUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Order(1)
@Component
class CorsFilter(
    @Value("\${application.security.allowed-origin:http://localhost:4200}")
    private val allowedOrigin: String,
) : WebFilter {

    private val allowedMethods = "GET, OPTIONS"
    private val maxAge = "3600"
    private val allowedHeaders = "x-requested-with, Content-Type, X-XSRF-TOKEN, x-api-key"

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request: ServerHttpRequest = exchange.request
        if (CorsUtils.isCorsRequest(request)) {
            val response: ServerHttpResponse = exchange.response
            val headers: HttpHeaders = response.headers
            headers.add("Access-Control-Allow-Origin", allowedOrigin)
            headers.add("Access-Control-Allow-Methods", allowedMethods)
            headers.add("Access-Control-Max-Age", maxAge)
            headers.add("Access-Control-Allow-Headers", allowedHeaders)
            if (request.method === HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK)
                return Mono.empty()
            }
        }
        return chain.filter(exchange)
    }
}
