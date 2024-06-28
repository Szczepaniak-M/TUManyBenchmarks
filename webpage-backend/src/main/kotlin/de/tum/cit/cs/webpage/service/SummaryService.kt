package de.tum.cit.cs.webpage.service

import com.github.benmanes.caffeine.cache.Caffeine
import de.tum.cit.cs.webpage.common.LoggerUtils.buildDebugLogMessage
import de.tum.cit.cs.webpage.model.Summary
import de.tum.cit.cs.webpage.repository.SummaryRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class SummaryService(
    private val summaryRepository: SummaryRepository,
) {

    private val logger = KotlinLogging.logger {}

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100)
        .build<String, Summary>()

    suspend fun findByInstanceType(instanceType: String, requestId: String?, apiKey: String?): Summary? {
        var summary = cache.getIfPresent(instanceType)
        if (summary == null) {
            logger.debug {
                buildDebugLogMessage("No summary found for $instanceType in the cache. Calling database.", requestId, apiKey)
            }
            summary = summaryRepository.findByInstanceName(instanceType)
            summary?.let {
                cache.put(instanceType, it)
                logger.debug {
                    buildDebugLogMessage("Summary for $instanceType found in the database and added to the cache.", requestId, apiKey)
                }
            }
        } else {
            logger.debug {
                buildDebugLogMessage("Summary for $instanceType founded in the cache.", requestId, apiKey)
            }
        }
        return summary
    }
}
