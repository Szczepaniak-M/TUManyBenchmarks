package de.tum.cit.cs.webpage.repository

import de.tum.cit.cs.webpage.MongoTestContainerConfig
import de.tum.cit.cs.webpage.model.Benchmark
import de.tum.cit.cs.webpage.model.BenchmarkResult
import de.tum.cit.cs.webpage.model.InstanceDetails
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

@DataMongoTest
@Testcontainers
@EnableReactiveMongoRepositories
@ContextConfiguration(classes = [MongoTestContainerConfig::class])
class InstanceDetailsRepositoryTests {

    @Autowired
    lateinit var instanceDetailsRepository: InstanceDetailsRepository

    companion object {
        private const val ID_1 = "id1"
        private const val ID_2 = "id2"
        private const val NAME_1 = "t3.micro"
        private const val NAME_2 = "c7gd.metal"
        private val TAGS_1 = listOf("2 vCPU", "1.0 GiB")
        private val TAGS_2 = listOf("64 vCPU", "128 GiB")
        private val BENCHMARKS_1 = listOf(
            Benchmark(
                "benchId1", "benchmark1", "benchmarkDescription1",
                listOf(BenchmarkResult(1000, mapOf(Pair("key1", "value1"))))
            ),
            Benchmark(
                "benchId2", "benchmark2", "benchmarkDescription2",
                listOf(BenchmarkResult(1000, mapOf(Pair("key2", "value2"))))
            )
        )
        private val BENCHMARKS_2 = listOf(
            Benchmark(
                "benchId3", "benchmark3", "benchmarkDescription3",
                listOf(BenchmarkResult(1000, mapOf(Pair("key3", "value3"))))
            )
        )
    }

    @Test
    fun `find instance details by instance name`() = runTest {
        // given
        val instanceDetails1 = InstanceDetails(ID_1, NAME_1, TAGS_1, BENCHMARKS_1)
        val instanceDetails2 = InstanceDetails(ID_2, NAME_2, TAGS_2, BENCHMARKS_2)
        instanceDetailsRepository.save(instanceDetails1)
        instanceDetailsRepository.save(instanceDetails2)

        // when
        val result = instanceDetailsRepository.findByName(NAME_1)

        // then
        assertNotNull(result)
        assertEquals(ID_1, result!!.id)
        assertEquals(NAME_1, result.name)
        assertEquals(TAGS_1, result.tags)
        assertEquals(BENCHMARKS_1, result.benchmarks)
    }

    @Test
    fun `not find instance details by instance name when it does not exists`() = runTest {
        // given
        val instanceDetails1 = InstanceDetails(ID_1, NAME_1, TAGS_1, BENCHMARKS_1)
        instanceDetailsRepository.save(instanceDetails1)

        // when
        val result = instanceDetailsRepository.findByName("nonExistingName")

        // then
        assertNull(result)
    }
}
