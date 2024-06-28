package de.tum.cit.cs.webpage.service

import com.github.benmanes.caffeine.cache.Caffeine
import de.tum.cit.cs.webpage.common.LoggerUtils.buildDebugLogMessage
import de.tum.cit.cs.webpage.model.Instance
import de.tum.cit.cs.webpage.repository.InstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class InstanceService(
    private val instanceRepository: InstanceRepository,
) {

    private val logger = KotlinLogging.logger {}

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(1)
        .build<String, List<Instance>>()

    suspend fun findAll(requestId: String?, apiKey: String?): Flow<Instance> {
        var instances = cache.getIfPresent("all")
        if (instances == null) {
            logger.debug {
                buildDebugLogMessage("Instance list not found in the cache. Calling database.", requestId, apiKey)
            }
            instances = instanceRepository.findAll().toList()
            cache.put("all", instances)
            logger.debug {
                buildDebugLogMessage("Instance list added to the cache.", requestId, apiKey)
            }
        } else {
            logger.debug {
                buildDebugLogMessage("Instance list founded in the cache.", requestId, apiKey)
            }
        }
        return instances.asFlow()
    }
}
