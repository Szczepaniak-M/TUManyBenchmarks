package de.tum.cit.cs.benchmarkservice.model

data class BenchmarkResult(
    val instanceId: String,
    val instanceName: String,
    val benchmarkId: String,
    val outputType: OutputType,
    val values: Map<String, Any>,
    val timestamp: Long,
) {
    fun toMongoModel(): BenchmarkResultMongo {
        return BenchmarkResultMongo(benchmarkId, timestamp, values)
    }
}

data class BenchmarkResultMongo(
    val benchmarkId: String,
    val timestamp: Long,
    val values: Map<String, Any>,
)
