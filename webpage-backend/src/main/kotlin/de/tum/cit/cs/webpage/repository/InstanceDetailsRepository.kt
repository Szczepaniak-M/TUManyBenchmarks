package de.tum.cit.cs.webpage.repository


import de.tum.cit.cs.webpage.model.InstanceDetails
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InstanceDetailsRepository : CoroutineCrudRepository<InstanceDetails, String> {
    suspend fun findByName(instanceName: String): InstanceDetails?
}
