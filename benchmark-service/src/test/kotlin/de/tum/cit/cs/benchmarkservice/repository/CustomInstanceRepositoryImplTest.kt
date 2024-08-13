package de.tum.cit.cs.benchmarkservice.repository

import de.tum.cit.cs.benchmarkservice.MongoTestContainerConfig
import de.tum.cit.cs.benchmarkservice.model.BenchmarkResult
import de.tum.cit.cs.benchmarkservice.model.Instance
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runTest
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal

@ActiveProfiles("test")
@DataMongoTest
@Testcontainers
@EnableReactiveMongoRepositories
@ContextConfiguration(classes = [MongoTestContainerConfig::class, CustomInstanceRepositoryImpl::class])
class CustomInstanceRepositoryImplTest {

    @Autowired
    lateinit var customInstanceRepository: CustomInstanceRepositoryImpl

    @Autowired
    lateinit var instanceRepository: InstanceRepository

    @Autowired
    lateinit var mongoTemplate: ReactiveMongoTemplate

    companion object {
        private const val ID_1 = "a6adce64cb23ae0c95e6bed6"
        private const val ID_2 = "722874f0b6b6600dae8dca9e"
        private const val NAME_1 = "t3.micro"
        private const val NAME_2 = "c7gd.metal"
        private const val CPU_1 = 2
        private const val CPU_2 = 64
        private val MEMORY_1 = BigDecimal(1)
        private val MEMORY_2 = BigDecimal(128)
        private const val NETWORK_1 = "Up to 25 Gigabit"
        private const val NETWORK_2 = "Up to 50 Gigabit"
        private val TAGS_1 = listOf("2 vCPU", "1.0 GiB", "Up to 25 Gigabit")
        private val TAGS_2 = listOf("64 vCPU", "128 GiB", "Up to 50 Gigabit")
        private const val BENCHMARK_ID_1 = "8327a14d7a8318ac3294d715"
        private const val BENCHMARK_ID_2 = "d881fbe239d23833ef47ef8a"
    }

    @Test
    fun `should return 0 if no benchmarks updated`() = runTest {
        // given
        val instance1 = Instance(ID_1, NAME_1, CPU_1, MEMORY_1, NETWORK_1, TAGS_1)
        val instance2 = Instance(ID_2, NAME_2, CPU_2, MEMORY_2, NETWORK_2, TAGS_2)
        instanceRepository.save(instance1)
        instanceRepository.save(instance2)

        // when
        val result = customInstanceRepository.updateBenchmarksById(ID_1, emptyList())

        // then
        assertEquals(0, result)
    }

    @Test
    fun `should push new benchmark result when benchmark is new`() = runTest {
        // given
        val instance1 = Instance(ID_1, NAME_1, CPU_1, MEMORY_1, NETWORK_1, TAGS_1)
        val instance2 = Instance(ID_2, NAME_2, CPU_2, MEMORY_2, NETWORK_2, TAGS_2)
        instanceRepository.save(instance1)
        instanceRepository.save(instance2)
        val benchmarkResult = listOf(
            BenchmarkResult(ID_1, NAME_1, BENCHMARK_ID_1, mapOf("key" to 100), 1000)
        )

        // when
        val result = customInstanceRepository.updateBenchmarksById(ID_1, benchmarkResult)

        // then
        assertEquals(1, result)

        val query = Query(Criteria.where("_id").`is`(ObjectId(ID_1)))
        val validation = mongoTemplate.find(query, InstanceWithBenchmarksMock::class.java, "instances").awaitSingle()
        assertEquals(1, validation.benchmarks?.size)
        assertEquals(BENCHMARK_ID_1, validation?.benchmarks!![0].benchmark)
        assertEquals(1, validation.benchmarks[0].results.size)
        assertEquals(1000, validation.benchmarks[0].results[0].timestamp)
        assertEquals(100, validation.benchmarks[0].results[0].values["key"])

        val querySecondInstance = Query(Criteria.where("_id").`is`(ObjectId(ID_2)))
        val secondInstance = mongoTemplate.find(querySecondInstance, InstanceWithBenchmarksMock::class.java, "instances").awaitSingle()
        assertNull(secondInstance.benchmarks)
    }

    @Test
    fun `should push new benchmark when benchmark is already exists`() = runTest {
        // given
        val instance1 = Instance(ID_1, NAME_1, CPU_1, MEMORY_1, NETWORK_1, TAGS_1)
        val instance2 = Instance(ID_2, NAME_2, CPU_2, MEMORY_2, NETWORK_2, TAGS_2)
        instanceRepository.save(instance1)
        instanceRepository.save(instance2)
        val benchmarkResultPrev = listOf(
            BenchmarkResult(ID_1, NAME_1, BENCHMARK_ID_1, mapOf("key" to 100), 1000)
        )
        customInstanceRepository.updateBenchmarksById(ID_1, benchmarkResultPrev)

        val benchmarkResult = listOf(
            BenchmarkResult(ID_1, NAME_1, BENCHMARK_ID_1, mapOf("key" to 200), 2000)
        )

        // when
        val result = customInstanceRepository.updateBenchmarksById(ID_1, benchmarkResult)

        // then
        assertEquals(1, result)
        val query = Query(Criteria.where("_id").`is`(ObjectId(ID_1)))
        val validation = mongoTemplate.find(query, InstanceWithBenchmarksMock::class.java, "instances").awaitSingle()
        assertEquals(1, validation.benchmarks?.size)
        assertEquals(BENCHMARK_ID_1, validation.benchmarks!![0].benchmark)
        assertEquals(2, validation.benchmarks[0].results.size)
        assertEquals(1000, validation.benchmarks[0].results[0].timestamp)
        assertEquals(100, validation.benchmarks[0].results[0].values["key"])
        assertEquals(2000, validation.benchmarks[0].results[1].timestamp)
        assertEquals(200, validation.benchmarks[0].results[1].values["key"])

        val querySecondInstance = Query(Criteria.where("_id").`is`(ObjectId(ID_2)))
        val secondInstance = mongoTemplate.find(querySecondInstance, InstanceWithBenchmarksMock::class.java, "instances").awaitSingle()
        assertNull(secondInstance.benchmarks)
    }

    @Test
    fun `should push new benchmark when the first benchmark is created and the second benchmark not`() = runTest {
        // given
        val instance1 = Instance(ID_1, NAME_1, CPU_1, MEMORY_1, NETWORK_1, TAGS_1)
        val instance2 = Instance(ID_2, NAME_2, CPU_2, MEMORY_2, NETWORK_2, TAGS_2)
        instanceRepository.save(instance1)
        instanceRepository.save(instance2)
        val benchmarkResultPrev = listOf(
            BenchmarkResult(ID_1, NAME_1, BENCHMARK_ID_1, mapOf("key" to 100), 1000)
        )
        customInstanceRepository.updateBenchmarksById(ID_1, benchmarkResultPrev)

        val benchmarkResult = listOf(
            BenchmarkResult(ID_1, NAME_1, BENCHMARK_ID_1, mapOf("key" to 200), 2000),
            BenchmarkResult(ID_1, NAME_1, BENCHMARK_ID_2, mapOf("key" to 300), 2000)
        )

        // when
        val result = customInstanceRepository.updateBenchmarksById(ID_1, benchmarkResult)

        // then
        assertEquals(2, result)
        val query = Query(Criteria.where("_id").`is`(ObjectId(ID_1)))
        val validation = mongoTemplate.find(query, InstanceWithBenchmarksMock::class.java, "instances").awaitSingle()
        assertEquals(2, validation.benchmarks?.size)
        assertEquals(BENCHMARK_ID_1, validation.benchmarks!![0].benchmark)
        assertEquals(2, validation.benchmarks[0].results.size)
        assertEquals(1000, validation.benchmarks[0].results[0].timestamp)
        assertEquals(100, validation.benchmarks[0].results[0].values["key"])
        assertEquals(2000, validation.benchmarks[0].results[1].timestamp)
        assertEquals(200, validation.benchmarks[0].results[1].values["key"])
        assertEquals(BENCHMARK_ID_2, validation.benchmarks[1].benchmark)
        assertEquals(1, validation.benchmarks[1].results.size)
        assertEquals(2000, validation.benchmarks[1].results[0].timestamp)
        assertEquals(300, validation.benchmarks[1].results[0].values["key"])

        val querySecondInstance = Query(Criteria.where("_id").`is`(ObjectId(ID_2)))
        val secondInstance = mongoTemplate.find(querySecondInstance, InstanceWithBenchmarksMock::class.java, "instances").awaitSingle()
        assertNull(secondInstance.benchmarks)
    }

    private data class InstanceWithBenchmarksMock(
        @Id
        val id: String?,
        val name: String?,
        val benchmarks: List<BenchmarkMock>?
    )

    private data class BenchmarkMock(
        val benchmark: String,
        val results: List<BenchmarkResultMock>
    )

    private data class BenchmarkResultMock(
        val values: Map<String, Any>,
        val timestamp: Long,
    )
}
