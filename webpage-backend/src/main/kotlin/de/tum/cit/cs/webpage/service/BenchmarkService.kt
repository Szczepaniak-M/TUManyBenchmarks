package de.tum.cit.cs.webpage.service

import com.github.benmanes.caffeine.cache.Caffeine
import de.tum.cit.cs.webpage.common.LoggerUtils.buildDebugLogMessage
import de.tum.cit.cs.webpage.model.BenchmarkDetails
import de.tum.cit.cs.webpage.model.BenchmarkStatistics
import de.tum.cit.cs.webpage.repository.BenchmarkDetailsRepository
import de.tum.cit.cs.webpage.repository.BenchmarkStatisticsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BenchmarkService(
    private val benchmarkDetailsRepository: BenchmarkDetailsRepository,
    private val benchmarkStatisticsRepository: BenchmarkStatisticsRepository,
) {

    private val logger = KotlinLogging.logger {}

    private val benchmarksCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(1)
        .build<String, List<BenchmarkDetails>>()

    private val statisticsCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(1)
        .build<String, List<BenchmarkStatistics>>()

    suspend fun findAllBenchmarkDetails(requestId: String?, apiKey: String?): Flow<BenchmarkDetails> {
        val benchmarks = benchmarksCache.getIfPresent("all")
        if (benchmarks == null) {
            logger.debug {
                buildDebugLogMessage("Benchmark list not found in the cache. Calling database.", requestId, apiKey)
            }
            val cachedList = mutableListOf<BenchmarkDetails>()
            val benchmarksFromDb = benchmarkDetailsRepository.findAll()
                .onEach { cachedList.add(it) }
                .onCompletion { benchmarksCache.put("all", cachedList) }

            logger.debug {
                buildDebugLogMessage("Benchmark list added to the cache.", requestId, apiKey)
            }
            return benchmarksFromDb
        } else {
            logger.debug {
                buildDebugLogMessage("Benchmark list founded in the cache.", requestId, apiKey)
            }
            return benchmarks.asFlow()
        }
    }

    suspend fun findAllStatistics(requestId: String?, apiKey: String?): Flow<BenchmarkStatistics> {
        val statistics = statisticsCache.getIfPresent("all")
        if (statistics == null) {
            logger.debug {
                buildDebugLogMessage(
                    "Benchmark statistics list not found in the cache. Calling database.",
                    requestId,
                    apiKey
                )
            }
            val cachedList = mutableListOf<BenchmarkStatistics>()
            val statisticsFromDb = benchmarkStatisticsRepository.findAll()
                .onEach { cachedList.add(it) }
                .onCompletion { statisticsCache.put("all", cachedList) }

            logger.debug {
                buildDebugLogMessage("Benchmark statistics list added to the cache.", requestId, apiKey)
            }
            return statisticsFromDb
        } else {
            logger.debug {
                buildDebugLogMessage("Benchmark statistics list founded in the cache.", requestId, apiKey)
            }
            return statistics.asFlow()
        }
    }
}