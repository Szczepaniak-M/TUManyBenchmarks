package de.tum.cit.cs.webpage.web

import de.tum.cit.cs.webpage.service.InstanceService
import de.tum.cit.cs.webpage.service.SummaryService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait


@RestController
class RestController(
    private val instanceService: InstanceService,
    private val summaryService: SummaryService
) {

    suspend fun listInstances(request: ServerRequest): ServerResponse {
        return ServerResponse.status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(instanceService.findAll())
    }

    suspend fun getInstance(request: ServerRequest): ServerResponse {
        val instanceType = request.pathVariable("instanceType")
        val summary = summaryService.findByInstanceType(instanceType)
        return if (summary == null) {
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait("{\"error\": \"instanceType not found\"}")
        } else {
            ServerResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(summary)
        }
    }
}
