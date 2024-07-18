package de.tum.cit.cs.benchmarkservice.model

import org.bson.types.ObjectId

data class BenchmarkResult(
    val instanceId: String,
    val instanceName: String,
    val benchmarkId: String,
    val outputType: OutputType,
    val values: Map<String, Any>,
    val timestamp: Long,
) {
    fun toMongoModel(): BenchmarkResultMongo {
        return BenchmarkResultMongo(ObjectId(benchmarkId), timestamp, values)
    }
}

data class BenchmarkResultMongo(
    val benchmark: ObjectId,
    val timestamp: Long,
    val values: Map<String, Any>,
)
