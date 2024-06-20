package de.tum.cit.cs.webpage.repository


import de.tum.cit.cs.webpage.model.Summary
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SummaryRepository : CoroutineCrudRepository<Summary, String> {
    suspend fun findByInstanceName(instanceName: String): Summary?
}
