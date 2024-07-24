package de.tum.cit.cs.webpage.web

import de.tum.cit.cs.webpage.common.Headers.X_FORWARDED_FOR_HEADER
import de.tum.cit.cs.webpage.model.ExplorerRequest
import de.tum.cit.cs.webpage.service.ApiKeyService
import de.tum.cit.cs.webpage.service.ExplorerService
import de.tum.cit.cs.webpage.service.InstanceDetailsService
import de.tum.cit.cs.webpage.service.InstanceService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.*
import kotlin.jvm.optionals.getOrNull


@RestController
class RestController(
    private val apiKeyService: ApiKeyService,
    private val instanceService: InstanceService,
    private val instanceDetailsService: InstanceDetailsService,
    private val explorerService: ExplorerService
) {

    suspend fun getApiKey(request: ServerRequest): ServerResponse {
        val ip = request.headers().firstHeader(X_FORWARDED_FOR_HEADER)
            ?: request.remoteAddress().getOrNull()?.address?.hostAddress

        if (ip.isNullOrBlank()) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait("{\"error\": \"No IP address found in the request\"}")
        }
        val requestId = request.attribute("requestId").getOrNull() as String?
        val apiKey = apiKeyService.generateApiKeyForIp(ip, requestId)
        return ServerResponse.status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait("{\"apiKey\": \"$apiKey\"}")
    }

    suspend fun listInstances(request: ServerRequest): ServerResponse {
        val requestId = request.attribute("requestId").getOrNull() as String?
        val apiKey = request.attribute("apiKey").getOrNull() as String?
        return ServerResponse.status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(instanceService.findAll(requestId, apiKey))
    }

    suspend fun getInstance(request: ServerRequest): ServerResponse {
        val instanceType = request.pathVariable("instanceType")
        val requestId = request.attribute("requestId").getOrNull() as String?
        val apiKey = request.attribute("apiKey").getOrNull() as String?
        val instanceDetails = instanceDetailsService.findByInstanceType(instanceType, requestId, apiKey)
        return if (instanceDetails == null) {
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait("{\"error\": \"instanceType not found\"}")
        } else {
            ServerResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(instanceDetails)
        }
    }

    suspend fun getExplorerResponse(request: ServerRequest): ServerResponse {
        val query = request.awaitBody(ExplorerRequest::class)
        val requestId = request.attribute("requestId").getOrNull() as String?
        val apiKey = request.attribute("apiKey").getOrNull() as String?
        val instanceDetails = explorerService.parseQuery(query, requestId, apiKey)
        return ServerResponse.status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(instanceDetails)
    }
}
