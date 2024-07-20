package de.tum.cit.cs.webpage.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.InstanceDetails
import de.tum.cit.cs.webpage.repository.InstanceDetailsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertNull

@ExtendWith(SpringExtension::class)
class InstanceDetailsServiceTest {

    @MockkBean
    private lateinit var instanceDetailsRepository: InstanceDetailsRepository

    @Test
    fun `find instance details by instance type and cache result`() = runTest {
        // given
        val service = InstanceDetailsService(instanceDetailsRepository)
        val instanceType = "t3.micro"
        val instanceDetails = InstanceDetails("id1", "t3.micro", emptyList(), emptyList())
        coEvery { instanceDetailsRepository.findByName(instanceType) } returns instanceDetails

        // when
        val databaseResult = service.findByInstanceType(instanceType, null, null)
        val cachedResult = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 1) { instanceDetailsRepository.findByName(instanceType) }
        assertNotNull(databaseResult)
        assertEquals(instanceDetails, databaseResult)
        assertNotNull(cachedResult)
        assertEquals(instanceDetails, databaseResult)
    }

    @Test
    fun `do not cache result if there is no instance details`() = runTest {
        // given
        val service = InstanceDetailsService(instanceDetailsRepository)
        val instanceType = "nonExisting"
        coEvery { instanceDetailsRepository.findByName(instanceType) } returns null

        // when
        val databaseResult1 = service.findByInstanceType(instanceType, null, null)
        val databaseResult2 = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 2) { instanceDetailsRepository.findByName(instanceType) }
        assertNull(databaseResult1)
        assertNull(databaseResult2)
    }
}
