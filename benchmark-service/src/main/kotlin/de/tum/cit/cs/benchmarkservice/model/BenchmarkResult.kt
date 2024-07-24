package de.tum.cit.cs.benchmarkservice.model

import org.bson.types.ObjectId

data class BenchmarkResult(
    val instanceId: String,
    val instanceName: String,
    val benchmarkId: String,
    val values: Map<String, Any>,
    val timestamp: Long,
) {
    fun toMongoBenchmark(): MongoBenchmark {
        return MongoBenchmark(ObjectId(benchmarkId), listOf(this.toMongoBenchmarkResult()))
    }
    fun toMongoBenchmarkResult(): MongoBenchmarkResult {
        return MongoBenchmarkResult(timestamp, values)
    }
}

data class MongoBenchmark(
    val benchmark: ObjectId,
    val results: List<MongoBenchmarkResult>,
)

data class MongoBenchmarkResult(
    val timestamp: Long,
    val values: Map<String, Any>,
)
