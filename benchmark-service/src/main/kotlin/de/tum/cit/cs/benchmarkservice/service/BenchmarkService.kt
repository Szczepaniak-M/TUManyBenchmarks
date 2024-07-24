package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.InstanceWithBenchmarks
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkCronRepository
import de.tum.cit.cs.benchmarkservice.repository.BenchmarkRepository
import de.tum.cit.cs.benchmarkservice.repository.CustomInstanceRepository
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class BenchmarkService(
    private val benchmarkCronRepository: BenchmarkCronRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val instanceRepository: InstanceRepository,
    private val customInstanceRepository: CustomInstanceRepository,
    private val cronParserService: CronParserService,
    private val instanceService: InstanceService,
    private val benchmarkRunnerService: BenchmarkRunnerService
) {

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalCoroutinesApi::class)
    fun runBenchmarks() = runBlocking {
        val benchmarkIdsToRun = getBenchmarksWithMatchingCron()
        val benchmarks = benchmarkRepository.findAllById(benchmarkIdsToRun).toList()
        logger.info { "Found ${benchmarks.size} benchmarks with matching CRON expression" }
        val instanceWithBenchmarksToRun = getInstancesWithBenchmarks(benchmarks)
        // execute multiple benchmarks in parallel without synchronizing
        val benchmarksCount = instanceWithBenchmarksToRun.flatMapMerge { instanceWithBenchmarks ->
            flow {
                emit(
                    async {
                        benchmarkRunnerService.runBenchmarksForInstance(instanceWithBenchmarks)
                    }.await()
                )
            }
        }
            .map { benchmarkResultsList ->
                if (benchmarkResultsList.isNotEmpty()) {
                    val instanceId = benchmarkResultsList[0].instanceId
                    val instanceName = benchmarkResultsList[0].instanceName
                    customInstanceRepository.updateBenchmarksById(instanceId, benchmarkResultsList)
                    logger.info { "Added ${benchmarkResultsList.size} benchmark results for instance' $instanceName'" }
                    benchmarkResultsList.size
                } else 0
            }
            .fold(0) { accumulator, value -> accumulator + value }
        logger.info { "Finished running $benchmarksCount benchmarks" }
    }

    private fun getBenchmarksWithMatchingCron(): Flow<String> {
        val now = ZonedDateTime.now().withMinute(0)
        return benchmarkCronRepository.findAll()
            .filter { cronParserService.isCronActive(it, now) }
            .map { it.id }
    }

    private fun getInstancesWithBenchmarks(benchmarks: List<Benchmark>): Flow<InstanceWithBenchmarks> {
        return instanceRepository.findAll()
            .filter { !it.tags.contains("Previous Generation") }
            .map { instanceService.findMatchingBenchmarks(it, benchmarks) }
            .filter { it.benchmarks.isNotEmpty() }
    }
}
