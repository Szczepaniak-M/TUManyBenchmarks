package de.tum.cit.cs.webpage.service

import com.github.benmanes.caffeine.cache.Caffeine
import de.tum.cit.cs.webpage.common.LoggerUtils.buildDebugLogMessage
import io.github.bucket4j.Bucket
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class ApiKeyService {
    @Value("\${application.security.bucket.size}")
    var bucketSize: Long = 60

    @Value("\${application.security.bucket.refill}")
    var refillSize: Long = 30

    private val logger = KotlinLogging.logger {}

    private val apiKeyToBucketCache = Caffeine.newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build<String, Bucket>()

    private val ipToApiKeyCache = Caffeine.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build<String, String>()

    private val random = SecureRandom()

    fun generateApiKeyForIp(ip: String, requestId: String?): String {
        var apiKey = ipToApiKeyCache.getIfPresent(ip)
        if (apiKey == null) {
            apiKey = generateApiKey()
            ipToApiKeyCache.put(ip, apiKey)
            apiKeyToBucketCache.put(apiKey,
                Bucket.builder()
                    .addLimit {
                        it.capacity(bucketSize)
                            .refillGreedy(refillSize, Duration.ofMinutes(1))
                    }
                    .build())
            logger.debug {
                buildDebugLogMessage("Generated a new ApiKey for IP $ip", requestId, null)
            }
        } else {
            logger.debug {
                buildDebugLogMessage("ApiKey found in the cache for IP $ip", requestId, null)
            }
        }
        return apiKey
    }

    fun isAccessAllowed(apiKey: String): Boolean? {
        val bucket = apiKeyToBucketCache.getIfPresent(apiKey)
        return bucket?.tryConsume(1)
    }

    private fun generateApiKey(): String {
        val apiKeyBytes = ByteArray(32)
        random.nextBytes(apiKeyBytes)
        return Base64.getUrlEncoder().encodeToString(apiKeyBytes)
    }
}
