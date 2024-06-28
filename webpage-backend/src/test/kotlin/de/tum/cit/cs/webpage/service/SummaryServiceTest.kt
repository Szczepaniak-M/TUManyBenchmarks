package de.tum.cit.cs.webpage.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.Summary
import de.tum.cit.cs.webpage.repository.SummaryRepository
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
class SummaryServiceTest {

    @MockkBean
    private lateinit var summaryRepository: SummaryRepository

    @Test
    fun `find summary by instance type and cache result`() = runTest {
        // given
        val service = SummaryService(summaryRepository)
        val instanceType = "t3.micro"
        val summary = Summary("id1", "t3.micro", emptyList(), emptyList())
        coEvery { summaryRepository.findByInstanceName(instanceType) } returns summary

        // when
        val databaseResult = service.findByInstanceType(instanceType, null, null)
        val cachedResult = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 1) { summaryRepository.findByInstanceName(instanceType) }
        assertNotNull(databaseResult)
        assertEquals(summary, databaseResult)
        assertNotNull(cachedResult)
        assertEquals(summary, databaseResult)
    }

    @Test
    fun `do not cache result if there is no summary`() = runTest {
        // given
        val service = SummaryService(summaryRepository)
        val instanceType = "nonExisting"
        coEvery { summaryRepository.findByInstanceName(instanceType) } returns null

        // when
        val databaseResult1 = service.findByInstanceType(instanceType, null, null)
        val databaseResult2 = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 2) { summaryRepository.findByInstanceName(instanceType) }
        assertNull(databaseResult1)
        assertNull(databaseResult2)
    }
}
