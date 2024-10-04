package de.tum.cit.cs.webpage.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "instance-details")
data class Instance(
    @Id
    val id: String?,
    val name: String,
    @Transient
    var onDemandPrice: BigDecimal = BigDecimal.ZERO,
    @Transient
    var spotPrice: BigDecimal = BigDecimal.ZERO,
    val vCpu: Int,
    val memory: BigDecimal,
    val network: String,
    val tags: List<String>,
    val benchmarks: List<Benchmark>
)

data class Benchmark(
    val id: String,
    val name: String,
    val description: String,
    val directory: String,
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
