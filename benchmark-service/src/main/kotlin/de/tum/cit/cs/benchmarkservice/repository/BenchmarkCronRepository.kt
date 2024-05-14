package de.tum.cit.cs.benchmarkservice.repository

import de.tum.cit.cs.benchmarkservice.model.BenchmarkCron
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BenchmarkCronRepository : CoroutineCrudRepository<BenchmarkCron, String>
