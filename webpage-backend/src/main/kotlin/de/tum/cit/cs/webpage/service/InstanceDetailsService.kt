package de.tum.cit.cs.webpage.service

import com.github.benmanes.caffeine.cache.Caffeine
import de.tum.cit.cs.webpage.common.LoggerUtils.buildDebugLogMessage
import de.tum.cit.cs.webpage.model.InstanceDetails
import de.tum.cit.cs.webpage.repository.InstanceDetailsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class InstanceDetailsService(
    private val instanceDetailsRepository: InstanceDetailsRepository,
) {

    private val logger = KotlinLogging.logger {}

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100)
        .build<String, InstanceDetails>()

    suspend fun findByInstanceType(instanceType: String, requestId: String?, apiKey: String?): InstanceDetails? {
        var instanceDetails = cache.getIfPresent(instanceType)
        if (instanceDetails == null) {
            logger.debug {
                buildDebugLogMessage("No instance details found for $instanceType in the cache. Calling database.", requestId, apiKey)
            }
            instanceDetails = instanceDetailsRepository.findByName(instanceType)
            instanceDetails?.let {
                cache.put(instanceType, it)
                logger.debug {
                    buildDebugLogMessage("Instance details for $instanceType found in the database and added to the cache.", requestId, apiKey)
                }
            }
        } else {
            logger.debug {
                buildDebugLogMessage("Instance details for $instanceType founded in the cache.", requestId, apiKey)
            }
        }
        return instanceDetails
    }
}
