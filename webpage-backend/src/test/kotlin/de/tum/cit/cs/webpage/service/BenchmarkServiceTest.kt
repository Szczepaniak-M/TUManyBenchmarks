package de.tum.cit.cs.webpage.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.BenchmarkDetails
import de.tum.cit.cs.webpage.model.BenchmarkStatistics
import de.tum.cit.cs.webpage.repository.BenchmarkDetailsRepository
import de.tum.cit.cs.webpage.repository.BenchmarkStatisticsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class BenchmarkServiceTest {

    private lateinit var service: BenchmarkService

    @MockkBean
    private lateinit var benchmarkDetailsRepository: BenchmarkDetailsRepository

    @MockkBean
    private lateinit var benchmarkStatisticsRepository: BenchmarkStatisticsRepository

    @BeforeEach
    fun setUp() {
        service = BenchmarkService(benchmarkDetailsRepository, benchmarkStatisticsRepository)
    }

    @Test
    fun `find all benchmark details and cache them`() = runTest {
        // given
        val benchmarkDetails1 = BenchmarkDetails("id1", "benchmarkName1", "benchmarkDescription1")
        val benchmarkDetails2 = BenchmarkDetails("id2", "benchmarkName2", "benchmarkDescription2")
        coEvery { benchmarkDetailsRepository.findAll() } returns flowOf(benchmarkDetails1, benchmarkDetails2)

        // when
        val databaseResult = service.findAllBenchmarkDetails(null, null).toList()
        val cachedResult = service.findAllBenchmarkDetails(null, null).toList()

        // then
        coVerify(exactly = 1) { benchmarkDetailsRepository.findAll() }
        assertThat(databaseResult).hasSameElementsAs(listOf(benchmarkDetails1, benchmarkDetails2))
        assertThat(cachedResult).hasSameElementsAs(listOf(benchmarkDetails1, benchmarkDetails2))
    }

    @Test
    fun `find all benchmark statistics and cache them`() = runTest {
        // given
        val benchmarkStatistics1 = BenchmarkStatistics("id1", "benchId1", "series1", 0.0, 0.0, 0.0,0.0)
        val benchmarkStatistics2 = BenchmarkStatistics("id2", "benchId2", "series2", 1.0, 1.0, 1.0,1.0)
        coEvery { benchmarkStatisticsRepository.findAll() } returns flowOf(benchmarkStatistics1, benchmarkStatistics2)

        // when
        val databaseResult = service.findAllStatistics(null, null).toList()
        val cachedResult = service.findAllStatistics(null, null).toList()

        // then
        coVerify(exactly = 1) { benchmarkStatisticsRepository.findAll() }
        assertThat(databaseResult).hasSameElementsAs(listOf(benchmarkStatistics1, benchmarkStatistics2))
        assertThat(cachedResult).hasSameElementsAs(listOf(benchmarkStatistics1, benchmarkStatistics2))
    }
}