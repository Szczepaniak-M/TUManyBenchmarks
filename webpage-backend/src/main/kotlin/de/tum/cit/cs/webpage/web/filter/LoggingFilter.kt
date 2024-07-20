package de.tum.cit.cs.webpage.web.filter

import de.tum.cit.cs.webpage.common.Headers.USER_AGENT
import de.tum.cit.cs.webpage.common.Headers.X_API_KEY_HEADER
import de.tum.cit.cs.webpage.common.Headers.X_FORWARDED_FOR_HEADER
import de.tum.cit.cs.webpage.common.LoggerUtils.buildInfoLogMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*

@Order(3)
@Component
class LoggingFilter : WebFilter {

    private val logger = KotlinLogging.logger {}

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        setRequestAttributes(exchange)
        logRequestDetails(exchange)
        return chain.filter(exchange)
            .doOnSuccess {
                logResponseDetails(exchange)
            }
    }

    private fun setRequestAttributes(exchange: ServerWebExchange) {
        val requestId = UUID.randomUUID().toString()
        exchange.attributes["requestId"] = requestId

        val apiKey = exchange.request.headers.getFirst(X_API_KEY_HEADER)
        apiKey?.let { exchange.attributes["apiKey"] = apiKey }
    }

    private fun logRequestDetails(exchange: ServerWebExchange) {
        val request = exchange.request
        val headers = request.headers
        val requestPath = "${request.method} ${request.path}"
        val sourceIp = headers.getFirst(X_FORWARDED_FOR_HEADER)
            ?: request.remoteAddress?.address?.hostAddress
        val agent = headers.getFirst(USER_AGENT) ?: "Unknown"
        val message = LogRequestDetails(requestPath, sourceIp, agent)
        logger.info {
            buildInfoLogMessage(
                message.toString(),
                exchange.getAttribute("requestId"),
                exchange.getAttribute("apiKey")
            )
        }
    }

    private fun logResponseDetails(exchange: ServerWebExchange) {
        val responseCode = exchange.response.statusCode
        logger.info {
            buildInfoLogMessage(
                "$responseCode",
                exchange.getAttribute("requestId"),
                exchange.getAttribute("apiKey")
            )
        }
    }
}

data class LogRequestDetails(
    val request: String,
    val sourceIp: String?,
    val agent: String,
) {
    override fun toString(): String {
        val ip = if (sourceIp == null) {
            "null"
        } else {
            "\"${sourceIp}\""
        }
        return "{ \"request\": \"$request\", " +
                "\"sourceIp\": $ip," +
                "\"agent\": \"$agent\" }"
    }
}
