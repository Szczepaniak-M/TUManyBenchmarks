package de.tum.cit.cs.benchmarkservice.model

data class InstanceWithBenchmarks(
    val instance: Instance,
    val benchmarks: List<Benchmark>
)
