package de.tum.cit.cs.benchmarkservice.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.*
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkCronRepository
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkRepository
import de.tum.cit.cs.benchmarkservice.repository.CustomInstanceRepository
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.ZonedDateTime

@ExtendWith(SpringExtension::class)
class BenchmarkServiceTest {

    private lateinit var benchmarkService: BenchmarkService

    @MockkBean
    private lateinit var benchmarkCronRepository: BenchmarkCronRepository

    @MockkBean
    private lateinit var benchmarkRepository: BenchmarkRepository

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @MockkBean
    private lateinit var customInstanceRepository: CustomInstanceRepository

    @MockkBean
    private lateinit var cronParserService: CronParserService

    @MockkBean
    private lateinit var instanceService: InstanceService

    @MockkBean
    private lateinit var benchmarkRunnerService: BenchmarkRunnerService

    @BeforeEach
    fun setUp() {
        benchmarkService = BenchmarkService(
            benchmarkCronRepository,
            benchmarkRepository,
            instanceRepository,
            customInstanceRepository,
            cronParserService,
            instanceService,
            benchmarkRunnerService
        )
    }

    @Test
    fun `should allow benchmark execution on initialization`() {
        assertTrue(benchmarkService.isBenchmarkExecutionAllowed())
    }

    @Test
    fun `should successfully forbidden benchmark execution and return true`() {
        // when
        val result = benchmarkService.stopFurtherBenchmarkExecutions()

        // then
        assertTrue(result)
        assertFalse(benchmarkService.isBenchmarkExecutionAllowed())
    }

    @Test
    fun `should successfully forbidden benchmark execution and return false if already forbidden`() {
        // when
        val result1 =benchmarkService.stopFurtherBenchmarkExecutions()
        val result2 = benchmarkService.stopFurtherBenchmarkExecutions()

        // then
        assertTrue(result1)
        assertFalse(result2)
        assertFalse(benchmarkService.isBenchmarkExecutionAllowed())
    }

    @Test
    fun `should run benchmarks and return correct number of benchmarks`() = runTest {
        // given
        val instances = listOf(
            Instance("id1", "t2.micro", 1, BigDecimal.ONE, "Low", emptyList()),
            Instance("id2", "t3.micro", 1, BigDecimal.ONE, "Low", emptyList()),
            Instance("id3", "t1.micro", 1, BigDecimal.ONE, "Low", listOf("Previous Generation")),
            Instance("id4", "t2.small", 1, BigDecimal.ONE, "Low", emptyList())
        )
        val benchmarks = listOf(
            Benchmark(
                "id1",
                Configuration("bench1", "bench1", "test1", "0 1 * * *", 1, emptyList(), listOf("t2.micro")),
                emptyList()
            ),
            Benchmark(
                "id2",
                Configuration("bench2", "bench2", "test2", "0 1 * * *", 1, emptyList(), listOf("t2.micro", "t3.micro")),
                emptyList()
            )
        )
        val instancesWithBenchmarks = listOf(
            InstanceWithBenchmarks(instances[0], listOf(benchmarks[0])),
            InstanceWithBenchmarks(instances[1], listOf(benchmarks[1])),
            InstanceWithBenchmarks(instances[3], emptyList())
        )

        coEvery { benchmarkCronRepository.findAll() } returns flowOf(
            BenchmarkCron("id1", "0 1 * * *"),
            BenchmarkCron("id2", "0 1 * * *"),
            BenchmarkCron("id3", "0 2 * * *")
        )

        every { cronParserService.isCronActive(any<BenchmarkCron>(), any<ZonedDateTime>()) } returnsMany listOf(
            true, true, false
        )

        coEvery { benchmarkRepository.findAllById(any<Flow<String>>()) } coAnswers {
            val benchmarkIds = firstArg<Flow<String>>().toList()
            assertEquals(2, benchmarkIds.size)
            assertEquals("id1", benchmarkIds[0])
            assertEquals("id2", benchmarkIds[1])
            benchmarks.asFlow()
        }

        coEvery { instanceRepository.findAll() } returns instances.asFlow()

        every {
            instanceService.findMatchingBenchmarks(
                any<Instance>(),
                any<List<Benchmark>>()
            )
        } returnsMany instancesWithBenchmarks

        coEvery { benchmarkRunnerService.runBenchmarksForInstance(instancesWithBenchmarks[0]) } returns listOf(
            BenchmarkResult("id1", "t2.micro", "id1", emptyMap(), 1),
            BenchmarkResult("id1", "t2.micro", "id2", emptyMap(), 1)
        )
        coEvery { benchmarkRunnerService.runBenchmarksForInstance(instancesWithBenchmarks[1]) } returns listOf(
            BenchmarkResult("id2", "t3.micro", "id2", emptyMap(), 1),
        )

        coEvery { customInstanceRepository.updateBenchmarksById("id1", any<List<BenchmarkResult>>()) } returns 2
        coEvery { customInstanceRepository.updateBenchmarksById("id2", any<List<BenchmarkResult>>()) } returns 1

        // when
        val result = benchmarkService.runBenchmarks()

        // then
        assertEquals(3, result)
        coVerify(exactly = 1) { benchmarkCronRepository.findAll() }
        coVerify(exactly = 3) { cronParserService.isCronActive(any<BenchmarkCron>(), any<ZonedDateTime>()) }
        coVerify(exactly = 1) { benchmarkRepository.findAllById(any<Flow<String>>()) }
        coVerify(exactly = 1) { instanceRepository.findAll() }
        verify(exactly = 3) { instanceService.findMatchingBenchmarks(any<Instance>(), any<List<Benchmark>>()) }
        coVerify(exactly = 2) { benchmarkRunnerService.runBenchmarksForInstance(any<InstanceWithBenchmarks>()) }
        coVerify(exactly = 2) { customInstanceRepository.updateBenchmarksById(any<String>(), any<List<BenchmarkResult>>()) }
    }

    @Test
    fun `should return 0 if no matching benchmarks`() = runTest {
        // given
        val instances = listOf(
            Instance("id1", "t2.micro", 1, BigDecimal.ONE, "Low", emptyList()),
        )
        val instancesWithBenchmarks = listOf(InstanceWithBenchmarks(instances[0], emptyList()))
        coEvery { benchmarkCronRepository.findAll() } returns flowOf(BenchmarkCron("id1", "0 1 * * *"))
        every { cronParserService.isCronActive(any<BenchmarkCron>(), any<ZonedDateTime>()) } returns false
        coEvery { benchmarkRepository.findAllById(any<Flow<String>>()) } coAnswers {
            val benchmarkIds = firstArg<Flow<String>>().toList()
            assertEquals(0, benchmarkIds.size)
            emptyFlow()
        }
        coEvery { instanceRepository.findAll() } returns instances.asFlow()
        every {
            instanceService.findMatchingBenchmarks(
                any<Instance>(),
                any<List<Benchmark>>()
            )
        } returnsMany instancesWithBenchmarks

        // when
        val result = benchmarkService.runBenchmarks()

        // then
        assertEquals(0, result)
        coVerify(exactly = 1) { benchmarkCronRepository.findAll() }
        verify(exactly = 1) { cronParserService.isCronActive(any<BenchmarkCron>(), any<ZonedDateTime>()) }
        coVerify(exactly = 1) { benchmarkRepository.findAllById(any<Flow<String>>()) }
        coVerify(exactly = 1) { instanceRepository.findAll() }
        verify(exactly = 1) { instanceService.findMatchingBenchmarks(any<Instance>(), any<List<Benchmark>>()) }
        coVerify(exactly = 0) { benchmarkRunnerService.runBenchmarksForInstance(any<InstanceWithBenchmarks>()) }
        coVerify(exactly = 0) { customInstanceRepository.updateBenchmarksById(any<String>(), any<List<BenchmarkResult>>()
        )
        }
    }

    @Test
    fun `should return 0 if all benchmark fails benchmarks`() = runTest {
        // given
        val instances = listOf(
            Instance("id1", "t2.micro", 1, BigDecimal.ONE, "Low", emptyList()),
            Instance("id2", "t3.micro", 1, BigDecimal.ONE, "Low", emptyList()),
            Instance("id3", "t1.micro", 1, BigDecimal.ONE, "Low", listOf("Previous Generation")),
            Instance("id4", "t2.small", 1, BigDecimal.ONE, "Low", emptyList())
        )
        val benchmarks = listOf(
            Benchmark(
                "id1",
                Configuration("bench1", "bench1", "test1", "0 1 * * *", 1, emptyList(), listOf("t2.micro")),
                emptyList()
            ),
            Benchmark(
                "id2",
                Configuration("bench2", "bench2", "test2", "0 1 * * *", 1, emptyList(), listOf("t2.micro", "t3.micro")),
                emptyList()
            )
        )
        val instancesWithBenchmarks = listOf(
            InstanceWithBenchmarks(instances[0], listOf(benchmarks[0])),
            InstanceWithBenchmarks(instances[1], listOf(benchmarks[1])),
            InstanceWithBenchmarks(instances[3], emptyList())
        )

        coEvery { benchmarkCronRepository.findAll() } returns flowOf(
            BenchmarkCron("id1", "0 1 * * *"),
            BenchmarkCron("id2", "0 1 * * *"),
            BenchmarkCron("id3", "0 2 * * *")
        )

        every { cronParserService.isCronActive(any<BenchmarkCron>(), any<ZonedDateTime>()) } returnsMany listOf(
            true, true, false
        )

        coEvery { benchmarkRepository.findAllById(any<Flow<String>>()) } coAnswers {
            val benchmarkIds = firstArg<Flow<String>>().toList()
            assertEquals(2, benchmarkIds.size)
            assertEquals("id1", benchmarkIds[0])
            assertEquals("id2", benchmarkIds[1])
            benchmarks.asFlow()
        }

        coEvery { instanceRepository.findAll() } returns instances.asFlow()

        every {
            instanceService.findMatchingBenchmarks(
                any<Instance>(),
                any<List<Benchmark>>()
            )
        } returnsMany instancesWithBenchmarks

        coEvery { benchmarkRunnerService.runBenchmarksForInstance(any<InstanceWithBenchmarks>()) } returns emptyList()

        // when
        val result = benchmarkService.runBenchmarks()

        // then
        assertEquals(0, result)
        coVerify(exactly = 1) { benchmarkCronRepository.findAll() }
        verify(exactly = 3) { cronParserService.isCronActive(any<BenchmarkCron>(), any<ZonedDateTime>()) }
        coVerify(exactly = 1) { benchmarkRepository.findAllById(any<Flow<String>>()) }
        coVerify(exactly = 1) { instanceRepository.findAll() }
        verify(exactly = 3) { instanceService.findMatchingBenchmarks(any<Instance>(), any<List<Benchmark>>()) }
        coVerify(exactly = 2) { benchmarkRunnerService.runBenchmarksForInstance(any<InstanceWithBenchmarks>()) }
        coVerify(exactly = 0) { customInstanceRepository.updateBenchmarksById(any<String>(), any<List<BenchmarkResult>>()) }
    }
}
