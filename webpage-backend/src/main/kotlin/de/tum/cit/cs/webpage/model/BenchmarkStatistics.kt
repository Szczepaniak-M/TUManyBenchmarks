package de.tum.cit.cs.webpage.model

import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "benchmark-statistics")
data class BenchmarkStatistics(
    val instanceId: String,
    val benchmarkId: String,
    val series: String,
    val min: Double,
    val max: Double,
    val avg: Double,
    val median: Double,
)
