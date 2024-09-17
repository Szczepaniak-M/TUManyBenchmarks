package de.tum.cit.cs.webpage.web

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.*
import de.tum.cit.cs.webpage.repository.BenchmarkDetailsRepository
import de.tum.cit.cs.webpage.repository.BenchmarkStatisticsRepository
import de.tum.cit.cs.webpage.repository.InstanceRepository
import de.tum.cit.cs.webpage.service.ApiKeyService
import de.tum.cit.cs.webpage.service.BenchmarkService
import de.tum.cit.cs.webpage.service.InstanceService
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
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal

@ActiveProfiles("test")
@WebFluxTest(controllers = [Router::class, RestController::class])
class RestControllerTest {

    @MockkBean
    private lateinit var instanceService: InstanceService

    @MockkBean
    private lateinit var apiKeyService: ApiKeyService

    @MockkBean
    private lateinit var benchmarkService: BenchmarkService

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @MockkBean
    private lateinit var benchmarkDetailsRepository: BenchmarkDetailsRepository

    @MockkBean
    private lateinit var benchmarkStatisticsRepository: BenchmarkStatisticsRepository

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
            Instance(
                "id1", "t3.micro", BigDecimal.ONE, BigDecimal.ONE, 1, BigDecimal.ONE, "Low", listOf("1 vCPU"),
                listOf(
                    Benchmark(
                        "benchId1", "benchmark1", "benchmarkDescription1",
                        listOf(BenchmarkResult(1000, mapOf(Pair("key1", 1)))),
                        listOf(
                            Plot(
                                "scatter", "Title 1", null, "Y",
                                listOf(PlotSeries(null, "key1", "legend1"))
                            )
                        )
                    ),
                    Benchmark(
                        "benchId2", "benchmark2", "benchmarkDescription2",
                        listOf(
                            BenchmarkResult(1000, mapOf(Pair("key2", listOf(1, 2, 3)), Pair("key3", listOf(4, 5, 6)))),
                            BenchmarkResult(1100, mapOf(Pair("key2", listOf(2, 3, 4)), Pair("key3", listOf(5, 6, 7))))
                        ),
                        listOf(
                            Plot(
                                "line", "Title 2", "X", "Y",
                                listOf(
                                    PlotSeries("increasingValues", "key2", "legend2"),
                                    PlotSeries("increasingValues", "key3", "legend3")
                                )
                            )
                        )
                    )
                )
            ),
            Instance(
                "id1", "t3.small", BigDecimal.TWO, BigDecimal.TWO, 2, BigDecimal.TWO, "Low", listOf("2 vCPU"),
                listOf(
                    Benchmark(
                        "benchId1", "benchmark1", "benchmarkDescription1",
                        listOf(BenchmarkResult(1000, mapOf(Pair("key1", 0.5)))),
                        listOf(
                            Plot(
                                "scatter", "Title 1", null, "Y",
                                listOf(PlotSeries(null, "key1", "legend1"))
                            )
                        )
                    ),
                    Benchmark(
                        "benchId2", "benchmark2", "benchmarkDescription2",
                        listOf(
                            BenchmarkResult(
                                1000,
                                mapOf(Pair("key2", listOf(0.5, 1, 1.5)), Pair("key3", listOf(2, 2.5, 3)))
                            ),
                            BenchmarkResult(
                                1100,
                                mapOf(Pair("key2", listOf(1, 1.5, 2)), Pair("key3", listOf(2.5, 3, 3.5)))
                            )
                        ),
                        listOf(
                            Plot(
                                "line", "Title 2", "X", "Y",
                                listOf(
                                    PlotSeries("increasingValues", "key2", "legend2"),
                                    PlotSeries("increasingValues", "key3", "legend3")
                                )
                            )
                        )
                    )
                )
            ),
        )
        coEvery { instanceService.findAll(any<String>(), apiKey) } returns instanceList.asFlow()
        every { apiKeyService.isAccessAllowed(apiKey) } returns true

        val expectedResponse = ClassPathResource("/responses/RestControllerTest/list-instances.json")
            .getContentAsString(Charsets.UTF_8)
            .trimIndent()

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
    fun `get instance details when instance type exists`() {
        // given
        val apiKey = "apiKey123"
        val instanceType = "t3.micro"
        val instance = Instance(
            "id1", "t3.small", BigDecimal.TWO, BigDecimal.TWO, 2, BigDecimal.TWO, "Low", listOf("2 vCPU"), listOf(
                Benchmark(
                    "benchId1", "benchmark1", "benchmarkDescription1",
                    listOf(BenchmarkResult(1000, mapOf(Pair("key1", 1)))),
                    listOf(
                        Plot(
                            "scatter", "Title 1", null, "Y",
                            listOf(PlotSeries(null, "key1", "legend1"))
                        )
                    )
                ),
                Benchmark(
                    "benchId2", "benchmark2", "benchmarkDescription2",
                    listOf(
                        BenchmarkResult(1000, mapOf(Pair("key2", listOf(1, 2, 3)), Pair("key3", listOf(4, 5, 6)))),
                        BenchmarkResult(1100, mapOf(Pair("key2", listOf(2, 3, 4)), Pair("key3", listOf(5, 6, 7))))
                    ),
                    listOf(
                        Plot(
                            "line", "Title 2", "X", "Y",
                            listOf(
                                PlotSeries("increasingValues", "key2", "legend2"),
                                PlotSeries("increasingValues", "key3", "legend3")
                            )
                        )
                    )
                )
            )
        )
        coEvery {
            instanceService.findByInstanceType(
                instanceType,
                any<String>(),
                apiKey
            )
        } returns instance
        every { apiKeyService.isAccessAllowed(apiKey) } returns true
        val expectedResponse = ClassPathResource("/responses/RestControllerTest/single-instance.json")
            .getContentAsString(Charsets.UTF_8)
            .trimIndent()

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
        coVerify(exactly = 1) { instanceService.findByInstanceType(instanceType, any<String>(), apiKey) }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get not found error response when instance type not exists`() {
        // given
        val apiKey = "apiKey123"
        val instanceType = "notExisting"
        coEvery { instanceService.findByInstanceType(instanceType, any<String>(), apiKey) } returns null
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
        coVerify(exactly = 1) { instanceService.findByInstanceType(instanceType, any<String>(), apiKey) }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get too many requests error response for get instance details request when access denied for given api key`() {
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
    fun `get unauthorized error response for get instance details request when api key no longer valid`() {
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
    fun `get unauthorized error response for get instance details request when no api key in headers`() {
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

    @Test
    fun `list benchmarks`() {
        // given
        val apiKey = "apiKey123"
        val benchmarkDetailsList = listOf(
            BenchmarkDetails(
                "id1", "name1", "description2",
                listOf("t2.micro", "t3.micro"),
                emptyList(),
                listOf("seriesX1"),
                listOf("seriesY1", "seriesY2"),
            ),
            BenchmarkDetails(
                "id1", "name1", "description2",
                emptyList(),
                listOf(listOf("tag1", "tag2"), listOf("tag3", "tag4")),
                listOf("seriesX2"),
                listOf("seriesY3", "seriesY4"),
            )
        )
        coEvery { benchmarkService.findAllBenchmarkDetails(any<String>(), apiKey) } returns benchmarkDetailsList.asFlow()
        every { apiKeyService.isAccessAllowed(apiKey) } returns true

        val expectedResponse = ClassPathResource("/responses/RestControllerTest/list-benchmarks.json")
            .getContentAsString(Charsets.UTF_8)
            .trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/benchmark")
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
        coVerify(exactly = 1) { benchmarkService.findAllBenchmarkDetails(any<String>(), apiKey) }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get too many requests error response for list benchmarks when access denied for given api key`() {
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
                it.path("/api/benchmark")
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
    fun `get unauthorized error response for list benchmarks when api key no longer valid`() {
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
                it.path("/api/benchmark")
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
    fun `get unauthorized error response for get list benchmark request when no api key in headers`() {
        // given
        val expectedResponse = """
                {
                   "error": "No valid API key found"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/benchmark")
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
    fun `list statistics`() {
        // given
        val apiKey = "apiKey123"
        val statisticsList = listOf(
            BenchmarkStatistics(
                "instanceId1", "benchmarkId1", "series1",
                1.0, 2.0, 3.0, 4.0
            ),
            BenchmarkStatistics(
                "instanceId2", "benchmarkId2", "series2",
                5.0, 6.0, 7.0, 8.0
            )
        )
        coEvery { benchmarkService.findAllStatistics(any<String>(), apiKey) } returns statisticsList.asFlow()
        every { apiKeyService.isAccessAllowed(apiKey) } returns true

        val expectedResponse = ClassPathResource("/responses/RestControllerTest/list-statistics.json")
            .getContentAsString(Charsets.UTF_8)
            .trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/statistics")
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
        coVerify(exactly = 1) { benchmarkService.findAllStatistics(any<String>(), apiKey) }
        verify(exactly = 1) { apiKeyService.isAccessAllowed(apiKey) }
    }

    @Test
    fun `get too many requests error response for list statistics when access denied for given api key`() {
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
                it.path("/api/statistics")
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
    fun `get unauthorized error response for list statistics when api key no longer valid`() {
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
                it.path("/api/statistics")
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
    fun `get unauthorized error response for get list statistics request when no api key in headers`() {
        // given
        val expectedResponse = """
                {
                   "error": "No valid API key found"
                }
                 """.trimIndent()

        // when-then
        webTestClient.get()
            .uri {
                it.path("/api/statistics")
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
