package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.Configuration
import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.model.OutputType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstanceServiceTest {

    companion object {
        private val TEMPLATE_INSTANCE = Instance(id = "id", name = "t2.micro", tags = emptyList())
        private val TEMPLATE_CONFIGURATION = Configuration(
            name = "", description = "", cron = "",
            outputType = OutputType.SINGLE_VALUE, instanceNumber = 0,
            instanceTags = null, instanceType = null
        )
        private val TEMPLATE_BENCHMARK = Benchmark(id = "", configuration = TEMPLATE_CONFIGURATION, nodes = emptyList())
    }

    private val instanceService = InstanceService()

    @Test
    fun `find matching benchmarks using instance type`() {
        // given
        val instance = TEMPLATE_INSTANCE
        val configuration = TEMPLATE_CONFIGURATION.copy(instanceType = listOf("t3.micro", "t2.micro"))
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(TEMPLATE_INSTANCE, result.instance)
    }

    @Test
    fun `find matching benchmarks using instance tags`() {
        // given
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceTags = listOf(
                listOf("8 vCPUs", "16 GiB Memory"),
                listOf("8 vCPUs", "8 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks while multiple instance tags match`() {
        // given
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceTags = listOf(
                listOf("8 vCPUs"),
                listOf("8 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks when no matches`() {
        // given
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceType = listOf("t3.small", "t2.small"),
            instanceTags = listOf(
                listOf("16 vCPUs"),
                listOf("16 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(0, result.benchmarks.size)
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks when instance type and instance tags match`() {
        // given
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceType = listOf("t3.small", "t2.micro"),
            instanceTags = listOf(
                listOf("16 vCPUs"),
                listOf("8 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks with only nulls`() {
        // given
        val instance = TEMPLATE_INSTANCE
        val benchmarks = listOf(TEMPLATE_BENCHMARK)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, benchmarks)

        // then
        assertEquals(0, result.benchmarks.size)
        assertEquals(TEMPLATE_INSTANCE, result.instance)
    }

    @Test
    fun `find matching benchmarks when multiple benchmarks match`() {
        // given
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration1 = TEMPLATE_CONFIGURATION.copy(instanceType = listOf("t2.micro"))
        val benchmark1 = TEMPLATE_BENCHMARK.copy(configuration = configuration1)
        val configuration2 = TEMPLATE_CONFIGURATION.copy(instanceTags = listOf(listOf("8 vCPUs")))
        val benchmark2 = TEMPLATE_BENCHMARK.copy(configuration = configuration2)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark1, benchmark2))

        // then
        assertEquals(2, result.benchmarks.size)
        assertEquals(benchmark1, result.benchmarks[0])
        assertEquals(benchmark2, result.benchmarks[1])
        assertEquals(instance, result.instance)
    }
}