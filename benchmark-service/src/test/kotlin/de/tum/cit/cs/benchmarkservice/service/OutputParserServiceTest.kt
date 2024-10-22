package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.Configuration
import de.tum.cit.cs.benchmarkservice.model.Instance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

class OutputParserServiceTest {

    private lateinit var outputParserService: OutputParserService

    @BeforeEach
    fun setUp() {
        outputParserService = OutputParserService()
    }

    @Test
    fun `should parse single output correctly`() {
        // given
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", "EBS only", listOf("ARM64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), emptyList()
        )
        val benchmark = Benchmark("benchmarkId", configuration, emptyList())
        val results = listOf("""{"key1": 1, "key2": [1, 2, 3]}""")

        // when
        val benchmarkResult = outputParserService.parseOutput(instance, benchmark, results)

        // then
        assertEquals(instance.id, benchmarkResult.instanceId)
        assertEquals(instance.name, benchmarkResult.instanceName)
        assertEquals(benchmark.id, benchmarkResult.benchmarkId)
        assertEquals(mapOf("key1" to 1, "key2" to listOf(1, 2, 3)), benchmarkResult.values)
        assertEquals(ZonedDateTime.now().withSecond(0).toEpochSecond(), benchmarkResult.timestamp)
    }

    @Test
    fun `should parse multiple outputs correctly`() {
        // Arrange
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", "EBS only", listOf("ARM64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), emptyList()
        )
        val benchmark = Benchmark("benchmarkId", configuration, emptyList())
        val results = listOf(
            """{"key1": 1}""",
            """{"key2": [1, 2, 3]}""",
            """{"key3": [2, 3, 4]}"""
        )

        // Act
        val benchmarkResult = outputParserService.parseOutput(instance, benchmark, results)

        // Assert
        assertEquals(instance.id, benchmarkResult.instanceId)
        assertEquals(instance.name, benchmarkResult.instanceName)
        assertEquals(benchmark.id, benchmarkResult.benchmarkId)
        assertEquals(mapOf("key1" to 1, "key2" to listOf(1, 2, 3), "key3" to listOf(2, 3, 4)), benchmarkResult.values)
        assertEquals(ZonedDateTime.now().withSecond(0).toEpochSecond(), benchmarkResult.timestamp)
    }

    @Test
    fun `should handle empty results list`() {
        // given
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", "EBS only", listOf("ARM64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), emptyList()
        )
        val benchmark = Benchmark("benchmarkId", configuration, emptyList())
        val results = emptyList<String>()

        // when
        val benchmarkResult = outputParserService.parseOutput(instance, benchmark, results)

        // then
        assertEquals(instance.id, benchmarkResult.instanceId)
        assertEquals(instance.name, benchmarkResult.instanceName)
        assertEquals(benchmark.id, benchmarkResult.benchmarkId)
        assertEquals(emptyMap<String, Any>(), benchmarkResult.values)
        assertEquals(ZonedDateTime.now().withSecond(0).toEpochSecond(), benchmarkResult.timestamp)
    }
}
