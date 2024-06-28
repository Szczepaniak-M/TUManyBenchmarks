package de.tum.cit.cs.webpage.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApiKeyServiceTest {

    @Test
    fun `generates new API key for new IP address`() {
        // given
        val ip = "192.168.0.1"
        val service = ApiKeyService()

        // when
        val result = service.generateApiKeyForIp(ip, null)

        // then
        assertNotNull(result)
    }

    @Test
    fun `return existing API key for given IP address`() {
        // given
        val ip = "192.168.0.1"
        val service = ApiKeyService()

        // when
        val result1 = service.generateApiKeyForIp(ip, null)
        val result2 = service.generateApiKeyForIp(ip, null)

        // then
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(result1, result2)
    }

    @Test
    fun `generate different keys for different IP addresses`() {
        // given
        val ip1 = "192.168.0.1"
        val ip2 = "192.168.0.2"
        val service = ApiKeyService()

        // when
        val result1 = service.generateApiKeyForIp(ip1, null)
        val result2 = service.generateApiKeyForIp(ip2, null)

        // then
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotEquals(result1, result2)
    }

    @Test
    fun `access is allowed for existing API key`() {
        // given
        val ip = "192.168.0.1"
        val service = ApiKeyService()

        // when
        val apiKey = service.generateApiKeyForIp(ip, null)
        val result = service.isAccessAllowed(apiKey)

        // then
        assertNotNull(result)
        assertTrue(result!!)
    }

    @Test
    fun `access is not allowed if too many requests key`() = runTest {
        // given
        val ip = "192.168.0.1"
        val service = ApiKeyService()
        service.bucketSize = 1

        // when
        val apiKey = service.generateApiKeyForIp(ip, null)
        service.isAccessAllowed(apiKey)
        val result = service.isAccessAllowed(apiKey)

        // then
        assertNotNull(result)
        assertFalse(result!!)
    }

    @Test
    fun `access is null for non-existing API key`() {
        // given
        val nonExistingAccessKey = "test"
        val service = ApiKeyService()

        // when
        val result = service.isAccessAllowed(nonExistingAccessKey)

        // then
        assertNull(result)
    }
}
