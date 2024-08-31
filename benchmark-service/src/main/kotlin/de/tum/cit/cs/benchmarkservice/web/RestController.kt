package de.tum.cit.cs.benchmarkservice.web

import de.tum.cit.cs.benchmarkservice.service.BenchmarkService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@RestController
class RestController(
    private val benchmarkService: BenchmarkService
) {
    suspend fun stopBenchmarkExecution(request: ServerRequest): ServerResponse {
        val isSuccess = benchmarkService.stopFurtherBenchmarkExecutions()
        return if (isSuccess) {
            return ServerResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait("""{"response": "Service will not schedule any new benchmarks"}""")
        } else {
            return ServerResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait("""{"response": "Benchmark scheduling is already stopped"}""")
        }
    }
}