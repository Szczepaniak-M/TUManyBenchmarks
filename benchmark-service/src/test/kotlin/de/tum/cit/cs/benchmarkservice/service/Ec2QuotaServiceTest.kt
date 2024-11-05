package de.tum.cit.cs.benchmarkservice.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class Ec2QuotaServiceTest {

    private lateinit var ec2QuotaService: Ec2QuotaService
    private val maxQuota = 10

    @BeforeEach
    fun setUp() {
        ec2QuotaService = Ec2QuotaService(maxQuota)
    }

    @Test
    fun `should acquire quota when sufficient quota available`() = runBlocking {
        // Given
        val acquiredQuota = 5

        // When
        val remainingQuota = ec2QuotaService.acquireQuota(acquiredQuota)

        // Then
        assertEquals(5, remainingQuota)
    }

    @Test
    fun `should release quota correctly`() = runBlocking {
        // Given
        val releasedQuota = 3

        // When
        val updatedQuota = ec2QuotaService.releaseQuota(releasedQuota)

        // Then
        assertEquals(13, updatedQuota)
    }

    @Test
    fun `should not acquire quota if insufficient quota and retry until available`() = runTest {
        // Given
        val acquiredQuota = 15

        // When
        val acquireJob = launch {
            val remainingQuota = ec2QuotaService.acquireQuota(acquiredQuota)
            assertEquals(5, remainingQuota)
        }
        ec2QuotaService.releaseQuota(10)

        // Then
        acquireJob.join()
    }

    @Test
    fun `should handle concurrent quota acquisition and release`() = runTest {
        // Given
        val acquiredQuota1 = 7
        val acquiredQuota2 = 5
        val acquiredQuota3 = 2

        // When
        val job1 = launch {
            val remainingQuota1 = ec2QuotaService.acquireQuota(acquiredQuota1)
            assertEquals(3, remainingQuota1)

            withContext(Dispatchers.Default) {
                delay(500)
            }

            val remainingQuota2 = ec2QuotaService.releaseQuota(acquiredQuota1)
            assertEquals(8, remainingQuota2)
        }

        val job2 = launch {
            val remainingQuota1 = ec2QuotaService.acquireQuota(acquiredQuota2)
            assertEquals(3, remainingQuota1)

            withContext(Dispatchers.Default) {
                delay(1500)
            }

            val remainingQuota2 = ec2QuotaService.releaseQuota(acquiredQuota2)
            assertEquals(10, remainingQuota2)
        }

        val job3 = launch {
            val remainingQuota1 = ec2QuotaService.acquireQuota(acquiredQuota3)
            assertEquals(1, remainingQuota1)

            withContext(Dispatchers.Default) {
                delay(1000)
            }

            val remainingQuota2 = ec2QuotaService.releaseQuota(acquiredQuota3)
            assertEquals(5, remainingQuota2)
        }

        // Then
        job1.join()
        job2.join()
        job3.join()
    }
}
