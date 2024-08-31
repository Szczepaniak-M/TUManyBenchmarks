package de.tum.cit.cs.benchmarkservice.web

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkCronRepository
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkRepository
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import de.tum.cit.cs.benchmarkservice.service.BenchmarkService
import io.mockk.every
import io.mockk.verify
import org.json.JSONException
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient

@ActiveProfiles("test")
@WebFluxTest(controllers = [Router::class, RestController::class])
class RestControllerTest {

    @MockkBean
    private lateinit var benchmarkService: BenchmarkService

    @MockkBean
    private lateinit var benchmarkCronRepository: BenchmarkCronRepository

    @MockkBean
    private lateinit var benchmarkRepository: BenchmarkRepository

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `stop benchmark scheduling on first request`() {
        // given
        every { benchmarkService.stopFurtherBenchmarkExecutions() } returns true
        val expectedResponse = """
            {
              "response": "Service will not schedule any new benchmarks"
            }
            """.trimIndent()

        // when-then
        webTestClient.delete()
            .uri {
                it.path("/api/service")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        verify(exactly = 1) { benchmarkService.stopFurtherBenchmarkExecutions() }
    }

    @Test
    fun `inform that scheduling is already stopped`() {
        // given
        every { benchmarkService.stopFurtherBenchmarkExecutions() } returns false
        val expectedResponse = """
            {
              "response": "Benchmark scheduling is already stopped"
            }
            """.trimIndent()

        // when-then
        webTestClient.delete()
            .uri {
                it.path("/api/service")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        verify(exactly = 1) { benchmarkService.stopFurtherBenchmarkExecutions() }
    }

    private fun assertJsonEquals(expected: String, actual: String?) {
        val expectedWithoutNewLine = expected.replace("\n".toRegex(), "")
        try {
            JSONAssert.assertEquals(expectedWithoutNewLine, actual, JSONCompareMode.STRICT)
        } catch (e: JSONException) {
            fail(e.message)
        }
    }

    private fun getBodyAsString(body: EntityExchangeResult<ByteArray?>): String {
        return String(body.responseBody ?: "".toByteArray())
    }
}