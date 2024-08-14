package de.tum.cit.cs.webpage.repository

import de.tum.cit.cs.webpage.model.BenchmarkDetails
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BenchmarkDetailsRepository : CoroutineCrudRepository<BenchmarkDetails, String>