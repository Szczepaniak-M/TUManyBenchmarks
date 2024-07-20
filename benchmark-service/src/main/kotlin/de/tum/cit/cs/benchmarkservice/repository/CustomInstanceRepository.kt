package de.tum.cit.cs.benchmarkservice.repository

import de.tum.cit.cs.benchmarkservice.model.BenchmarkResult

interface CustomInstanceRepository {
    suspend fun updateBenchmarksById(instance: String, benchmarks: List<BenchmarkResult>): Int
}
