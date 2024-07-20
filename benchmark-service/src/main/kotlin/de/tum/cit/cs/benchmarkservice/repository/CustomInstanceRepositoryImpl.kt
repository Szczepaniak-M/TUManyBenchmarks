package de.tum.cit.cs.benchmarkservice.repository

import com.mongodb.client.model.*
import de.tum.cit.cs.benchmarkservice.model.BenchmarkResult
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository


@Repository
class CustomInstanceRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : CustomInstanceRepository {

    override suspend fun updateBenchmarksById(instance: String, benchmarks: List<BenchmarkResult>): Int {
        val bulkOperations = mutableListOf<WriteModel<Document>>()
        val instanceId = ObjectId(instance)
        for (benchmark in benchmarks) {
            val benchmarkId = ObjectId(benchmark.benchmarkId)
            if (isBenchmarkCreated(instanceId, benchmarkId)) {
                pushNewBenchmarkResult(instanceId, benchmarkId, benchmark, bulkOperations)
            } else {
                pushNewBenchmark(instanceId, benchmark, bulkOperations)
            }
        }
        val collection = mongoTemplate.getCollection("instances").awaitSingle()
        val bulkResult = collection.bulkWrite(bulkOperations, BulkWriteOptions().ordered(false)).awaitSingle()
        return bulkResult.modifiedCount
    }

    private suspend fun isBenchmarkCreated(instanceId: ObjectId, benchmarkId: ObjectId): Boolean {
        val benchmarkQuery = Query(
            Criteria.where("_id").`is`(instanceId)
                .and("benchmarks.benchmark").`is`(benchmarkId)
        )
        return mongoTemplate.exists(benchmarkQuery, "instances").awaitSingle()
    }

    private fun pushNewBenchmarkResult(
        instanceId: ObjectId,
        benchmarkId: ObjectId,
        benchmarkResult: BenchmarkResult,
        bulkOperations: MutableList<WriteModel<Document>>
    ) {
        val query = Filters.eq("_id", instanceId)
        val mongoBenchmarkResult = Document()
        mongoTemplate.converter.write(benchmarkResult.toMongoBenchmarkResult(), mongoBenchmarkResult)
        mongoBenchmarkResult.remove("_class")
        val update = Updates.push("benchmarks.\$[benchmark].results", mongoBenchmarkResult)
        val updateOptions = UpdateOptions()
            .arrayFilters(mutableListOf(Filters.eq("benchmark.benchmark", benchmarkId)))
        bulkOperations.add(UpdateOneModel(query, update, updateOptions))
    }

    private fun pushNewBenchmark(
        instanceId: ObjectId,
        benchmarkResult: BenchmarkResult,
        bulkOperations: MutableList<WriteModel<Document>>
    ) {
        val query = Filters.eq("_id", instanceId)
        val mongoBenchmark = Document()
        mongoTemplate.converter.write(benchmarkResult.toMongoBenchmark(), mongoBenchmark)
        mongoBenchmark.remove("_class")
        val update = Updates.push("benchmarks", mongoBenchmark)
        bulkOperations.add(UpdateOneModel(query, update))
    }
}
