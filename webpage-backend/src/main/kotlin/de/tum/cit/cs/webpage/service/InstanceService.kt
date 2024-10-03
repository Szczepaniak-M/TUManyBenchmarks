package de.tum.cit.cs.webpage.service

import com.github.benmanes.caffeine.cache.Caffeine
import de.tum.cit.cs.webpage.common.LoggerUtils.buildDebugLogMessage
import de.tum.cit.cs.webpage.model.Instance
import de.tum.cit.cs.webpage.repository.InstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class InstanceService(
    private val instanceRepository: InstanceRepository,
    private val ec2PriceService: Ec2PriceService
) {

    private val logger = KotlinLogging.logger {}

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(1000)
        .build<String, Instance>()

    fun findAll(requestId: String?, apiKey: String?): Flow<Instance> {
        val instances = cache.asMap().values
        if (instances.isEmpty()) {
            logger.debug {
                buildDebugLogMessage("Instance list not found in the cache. Calling database.", requestId, apiKey)
            }
            val instancesFromDb = instanceRepository.findAll()
                .onEach {
                    it.onDemandPrice = ec2PriceService.getOnDemandPrice(it.name)
                    it.spotPrice = ec2PriceService.getSpotPrice(it.name)
                    cache.put(it.name, it)
                }

            logger.debug {
                buildDebugLogMessage("Instance list added to the cache.", requestId, apiKey)
            }
            return instancesFromDb
        } else {
            logger.debug {
                buildDebugLogMessage("Instance list founded in the cache.", requestId, apiKey)
            }
            return instances.asFlow()
        }
    }

    suspend fun findByInstanceType(instanceType: String, requestId: String?, apiKey: String?): Instance? {
        var instanceDetails = cache.getIfPresent(instanceType)
        if (instanceDetails == null) {
            logger.debug {
                buildDebugLogMessage(
                    "No instance details found for $instanceType in the cache. Calling database.",
                    requestId,
                    apiKey
                )
            }
            instanceDetails = instanceRepository.findByName(instanceType)
            instanceDetails?.let {
                it.onDemandPrice = ec2PriceService.getOnDemandPrice(instanceType)
                it.spotPrice = ec2PriceService.getSpotPrice(instanceType)
                logger.debug {
                    buildDebugLogMessage(
                        "Instance details for $instanceType found in the database and added to the cache.",
                        requestId,
                        apiKey
                    )
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
