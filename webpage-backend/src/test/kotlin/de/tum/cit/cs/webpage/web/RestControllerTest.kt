package de.tum.cit.cs.webpage.web

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.BenchmarkResult
import de.tum.cit.cs.webpage.model.Instance
import de.tum.cit.cs.webpage.model.Summary
import de.tum.cit.cs.webpage.repository.InstanceRepository
import de.tum.cit.cs.webpage.repository.SummaryRepository
import de.tum.cit.cs.webpage.service.InstanceService
import de.tum.cit.cs.webpage.service.SummaryService
import io.mockk.coEvery
import io.mockk.coVerify
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

@Suppress("UNUSED_PROPERTY")
@WebFluxTest(controllers = [Router::class, RestController::class])
class RestControllerTest {

    @MockkBean
    private lateinit var instanceService: InstanceService

    @MockkBean
    private lateinit var summaryService: SummaryService

    @MockkBean
    private lateinit var summaryRepository: SummaryRepository

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun listInstances() {
        // given
        val instanceList = listOf(
            Instance("id1", "t3.micro", listOf("1 vCPU")),
            Instance("id2", "t3.small", listOf("2 vCPU"))
        )
        coEvery { instanceService.findAll() } returns instanceList.asFlow()
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
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        coVerify { instanceService.findAll() }
    }

    @Test
    fun `get summary when instance type exists`() {
        // given
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
        coEvery { summaryService.findByInstanceType(instanceType) } returns summary
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
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        coVerify { summaryService.findByInstanceType(instanceType) }
    }

    @Test
    fun `get not found response when instance type not exists`() {
        // given
        val instanceType = "notExisting"
        coEvery { summaryService.findByInstanceType(instanceType) } returns null
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
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody()
            .consumeWith {
                assertJsonEquals(
                    expectedResponse,
                    getBodyAsString(it)
                )
            }
        coVerify { summaryService.findByInstanceType(instanceType) }
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