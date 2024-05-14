package de.tum.cit.cs.benchmarkservice.repository

import de.tum.cit.cs.benchmarkservice.model.Instance
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InstanceRepository : CoroutineCrudRepository<Instance, String>
