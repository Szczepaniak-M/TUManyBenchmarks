package de.tum.cit.cs.webpage.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "instance-details")
data class InstanceDetails(
    @Id
    val id: String?,
    val name: String,
    val tags: List<String>,
    val benchmarks: List<Benchmark>
)

data class Benchmark(
    val id: String,
    val name: String,
    val description: String,
    val results: List<BenchmarkResult>
)

data class BenchmarkResult(
    val timestamp: Long,
    val values: Map<String, Any>,
)
