package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.InstanceWithBenchmarks
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkCronRepository
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkRepository
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class BenchmarkService(
    val benchmarkCronRepository: BenchmarkCronRepository,
    val benchmarkRepository: BenchmarkRepository,
    val instanceRepository: InstanceRepository,
    val cronParserService: CronParserService,
    val instanceService: InstanceService,
    val benchmarkRunnerService: BenchmarkRunnerService
) {

    private val logger = KotlinLogging.logger {}

    fun runBenchmarks() = runBlocking {
        val benchmarkIdsToRun = getBenchmarksWithMatchingCron()
        val benchmarks = benchmarkRepository.findAllById(benchmarkIdsToRun).toList()
        logger.info { "Found ${benchmarks.size} benchmarks with matching CRON expression" }
        val instanceWithBenchmarksToRun = getInstancesWithBenchmarks(benchmarks)
        val benchmarkResults = instanceWithBenchmarksToRun.map(benchmarkRunnerService::runBenchmarksForInstance)
    }

    private fun getBenchmarksWithMatchingCron(): Flow<String> {
        val now = ZonedDateTime.now().withMinute(0)
        return benchmarkCronRepository.findAll()
            .filter { cronParserService.isCronActive(it, now) }
            .map { it.id }
    }

    private fun getInstancesWithBenchmarks(benchmarks: List<Benchmark>): Flow<InstanceWithBenchmarks> {
        return instanceRepository.findAll()
            .map { instanceService.findMatchingBenchmarks(it, benchmarks) }
            .filter { it.benchmarks.isNotEmpty() }
    }
}
