package de.tum.cit.cs.benchmarkservice.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

private const val TEST_PORT = 8082

@ActiveProfiles("test")
@AutoConfigureWireMock(port = TEST_PORT)
@TestPropertySource(properties = ["github.repository.url=http://localhost:${TEST_PORT}"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GitHubServiceTest {

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @Autowired
    private lateinit var gitHubService: GitHubService

    @Test
    fun `generate curl for all files and their subdirectories`() = runTest {
        // Given
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/benchmark"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                        .withBody(
                            ClassPathResource("/responses/GitHubService/response_benchmark.json")
                                .getContentAsString(Charsets.UTF_8)
                        )
                )
        )
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/benchmark/src"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                        .withBody(
                            ClassPathResource("/responses/GitHubService/response_benchmark_src.json")
                                .getContentAsString(Charsets.UTF_8)
                        )
                )
        )

        // When
        val result = gitHubService.getCurlsForFilesFromDirectory("/benchmark")

        // Then
        assertEquals(3, result.size)
        assertEquals(
            listOf(
                "curl --create-dirs -o \"benchmark/README.md\" \"https://raw.githubusercontent.com/MyUserName/MyRepository/main/benchmark/README.md\"",
                "curl --create-dirs -o \"benchmark/Makefile\" \"https://raw.githubusercontent.com/MyUserName/MyRepository/main/benchmark/Makefile\"",
                "curl --create-dirs -o \"benchmark/src/main.c\" \"https://raw.githubusercontent.com/MyUserName/MyRepository/main/benchmark/src/main.c\"",
            ),
            result
        )

    }
}
