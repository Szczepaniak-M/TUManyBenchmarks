package de.tum.cit.cs.webpage.service

import com.github.benmanes.caffeine.cache.Caffeine
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

    suspend fun findByInstanceType(instanceType: String): Summary? {
        var summary = cache.getIfPresent(instanceType)
        if (summary == null) {
            logger.debug { "No summary found for $instanceType in the cache. Calling database." }
            summary = summaryRepository.findByInstanceName(instanceType)
            summary?.let {
                cache.put(instanceType, it)
                logger.debug { "Summary for $instanceType found in the database and added to the cache." }
            }
        } else {
            logger.debug { "Summary for $instanceType founded in the cache." }
        }
        return summary
    }
}
