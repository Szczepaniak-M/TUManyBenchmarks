package de.tum.cit.cs.webpage.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "summaries")
data class Summary(
    @Id
    val id: String?,
    val instanceName: String,
    val tags: List<String>,
    val benchmarks: List<BenchmarkResult>
)

data class BenchmarkResult(
    val benchmarkName: String,
    val benchmarkDescription: String,
    val outputType: String,
    val timestamp: Long,
    val values: Map<String, String>,
)
