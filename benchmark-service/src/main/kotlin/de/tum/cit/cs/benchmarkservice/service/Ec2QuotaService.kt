package de.tum.cit.cs.benchmarkservice.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class Ec2QuotaService(
    @Value("\${aws.ec2.quota}")
    private val maxQuota: Int
) {
    private val mutex: Mutex = Mutex()
    private val currentQuota: AtomicInteger = AtomicInteger(maxQuota)

    suspend fun acquireQuota(acquiredQuota: Int): Int {
        while (true) {
            mutex.withLock {
                if (currentQuota.get() > acquiredQuota) {
                    return currentQuota.addAndGet(-acquiredQuota)
                }
            }
            delay(30_000)
        }
    }

    suspend fun releaseQuota(releasedQuota: Int): Int {
        mutex.withLock {
            return currentQuota.addAndGet(releasedQuota)
        }
    }
}