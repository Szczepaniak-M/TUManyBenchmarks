package de.tum.cit.cs.webpage.repository

import de.tum.cit.cs.webpage.model.BenchmarkStatistics
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BenchmarkStatisticsRepository : CoroutineCrudRepository<BenchmarkStatistics, String>