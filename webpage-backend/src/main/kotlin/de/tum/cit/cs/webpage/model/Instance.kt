package de.tum.cit.cs.webpage.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "instance-details")
data class Instance(
    @Id
    val id: String?,
    val name: String,
    val vCpu: Int,
    val memory: Double,
    val network: String,
    val tags: List<String>,
    val benchmarks: List<Benchmark>
)

data class Benchmark(
    val id: String,
    val name: String,
    val description: String,
    val statistics: Map<String, Any>,
    val results: List<BenchmarkResult>,
    val plots: List<Plot>
)

data class BenchmarkResult(
    val timestamp: Long,
    val values: Map<String, Any>,
)

data class Plot(
    val type: String,
    val title: String,
    val xaxis: String?,
    val yaxis: String,
    val series: List<PlotSeries>
)

data class PlotSeries(
    val x: String?,
    val y: String,
    val legend: String
)
