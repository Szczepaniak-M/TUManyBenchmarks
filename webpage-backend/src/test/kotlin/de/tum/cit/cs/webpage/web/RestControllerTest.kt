package de.tum.cit.cs.webpage.web

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.BenchmarkResult
import de.tum.cit.cs.webpage.model.Instance
import de.tum.cit.cs.webpage.model.Summary
import de.tum.cit.cs.webpage.repository.InstanceRepository
import de.tum.cit.cs.webpage.repository.SummaryRepository
import de.tum.cit.cs.webpage.service.ApiKeyService
import de.tum.cit.cs.webpage.service.InstanceService
import de.tum.cit.cs.webpage.service.SummaryService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.asFlow
import org.json.JSONException
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(controllers = [Router::class, RestController::class])
class RestControllerTest {

    @MockkBean
    private lateinit var instanceService: InstanceService

    @MockkBean
    private lateinit var summaryService: SummaryService

    @MockkBean
    private lateinit var apiKeyService: ApiKeyService

    @MockkBean
    private lateinit var summaryRepository: SummaryRepository

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `get api key when IP found`() {
        // given
        val ip = "192.168.0.1"
        every { apiKeyService.generateApiKeyForIp(ip, any<String>()) } returns "apiKey123"
        val expectedResponse = """
            {
                "apiKey": "apiKey123"
            }
            """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/key")
                    .build()
            }
            .header("X-Forwarded-For", ip)
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
        verify(exactly = 1) { apiKeyService.generateApiKeyForIp(ip, any<String>()) }
    }

    @Test
    fun `get bad request error response when no IP address in request`() {
        // given
        val expectedResponse = """
            {
                "error": "No IP address found in the request"
            }
            """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/key")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
    }

    @Test
    fun `list instances`() {
        // given
        val apiKey = "apiKey123"
        val instanceList = listOf(
            Instance("id1", "t3.micro", listOf("1 vCPU")),
            Instance("id2", "t3.small", listOf("2 vCPU"))
        )
        coEvery { instanceService.findAll(any<String>(), apiKey) } returns instanceList.asFlow()
        every { apiKeyService.isAccessAllowed(apiKey) } returns true

        val expectedResponse = """
            [
                {
                    "id": "id1",
                    "name": "t3.micro",
                    "tags": [
                        "1 vCPU"
                    ]
                },
                {
                    "id": "id2",
                    "name": "t3.small",
                    "tags": [
                        "2 vCPU"
                    ]
                }
            ]
            """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .header("X-API-Key", apiKey)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        coVerify(exactly = 1) { instanceService.findAll(any<String>(), apiKey) }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get too many requests error response for list instance when access denied for given api key`() {
        // given
        val apiKey = "apiKey123"
        every { apiKeyService.isAccessAllowed(apiKey) } returns false

        val expectedResponse = """
                {
                   "error": "Too many requests in one minute"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .header("X-API-Key", apiKey)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get unauthorized error response for list instance when api key no longer valid`() {
        // given
        val apiKey = "apiKey123"
        every { apiKeyService.isAccessAllowed(apiKey) } returns null

        val expectedResponse = """
                {
                   "error": "No valid API key found"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .header("X-API-Key", apiKey)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get unauthorized error response for get list request when no api key in headers`() {
        // given
        val expectedResponse = """
                {
                   "error": "No valid API key found"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
    }

    @Test
    fun `get summary when instance type exists`() {
        // given
        val apiKey = "apiKey123"
        val instanceType = "t3.micro"
        val benchmarkResults = listOf(
            BenchmarkResult(
                "benchmark1", "benchmarkDescription1",
                "outputType1", 1000, mapOf(Pair("key1", "value1"))
            ),
            BenchmarkResult(
                "benchmark2", "benchmarkDescription2",
                "outputType2", 1000, mapOf(Pair("key2", "value2"))
            )
        )
        val summary = Summary("id1", "t3.micro", listOf("1 vCPU"), benchmarkResults)
        coEvery { summaryService.findByInstanceType(instanceType, any<String>(), apiKey) } returns summary
        every { apiKeyService.isAccessAllowed(apiKey) } returns true
        val expectedResponse = """
            {
                "id": "id1",
                "instanceName": "t3.micro",
                "tags": [
                    "1 vCPU"
                ],
                "benchmarks": [
                    {
                        "benchmarkName": "benchmark1",
                        "benchmarkDescription": "benchmarkDescription1",
                        "outputType": "outputType1",
                        "timestamp": 1000,
                        "values": {
                            "key1":"value1"
                        }
                    },
                    {
                        "benchmarkName":"benchmark2",
                        "benchmarkDescription":"benchmarkDescription2",
                        "outputType":"outputType2",
                        "timestamp":1000,
                        "values":{
                            "key2":"value2"
                        }
                     }
                ]
            }
            """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance/{instanceType}")
                    .build(instanceType)
            }
            .accept(MediaType.APPLICATION_JSON)
            .header("X-API-Key", apiKey)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        coVerify(exactly = 1) { summaryService.findByInstanceType(instanceType, any<String>(), apiKey) }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get not found error response when instance type not exists`() {
        // given
        val apiKey = "apiKey123"
        val instanceType = "notExisting"
        coEvery { summaryService.findByInstanceType(instanceType, any<String>(), apiKey) } returns null
        every { apiKeyService.isAccessAllowed(apiKey) } returns true

        val expectedResponse = """
                {
                   "error": "instanceType not found"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance/{instanceType}")
                    .build(instanceType)
            }
            .accept(MediaType.APPLICATION_JSON)
            .header("X-API-Key", apiKey)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        coVerify(exactly = 1) { summaryService.findByInstanceType(instanceType, any<String>(), apiKey) }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get too many requests error response for get summary request when access denied for given api key`() {
        // given
        val apiKey = "apiKey123"
        val instanceType = "t3.micro"
        every { apiKeyService.isAccessAllowed(apiKey) } returns false

        val expectedResponse = """
                {
                   "error": "Too many requests in one minute"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance/{instanceType}")
                    .build(instanceType)
            }
            .accept(MediaType.APPLICATION_JSON)
            .header("X-API-Key", apiKey)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get unauthorized error response for get summary request when api key no longer valid`() {
        // given
        val apiKey = "apiKey123"
        val instanceType = "t3.micro"
        every { apiKeyService.isAccessAllowed(apiKey) } returns null

        val expectedResponse = """
                {
                   "error": "No valid API key found"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance/{instanceType}")
                    .build(instanceType)
            }
            .accept(MediaType.APPLICATION_JSON)
            .header("X-API-Key", apiKey)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get unauthorized error response for get summary request when no api key in headers`() {
        // given
        val instanceType = "t3.micro"
        val expectedResponse = """
                {
                   "error": "No valid API key found"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/instance/{instanceType}")
                    .build(instanceType)
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
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
