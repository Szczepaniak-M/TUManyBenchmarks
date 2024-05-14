package de.tum.cit.cs.benchmarkservice.repository

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BenchmarkRepository : CoroutineCrudRepository<Benchmark, String>
