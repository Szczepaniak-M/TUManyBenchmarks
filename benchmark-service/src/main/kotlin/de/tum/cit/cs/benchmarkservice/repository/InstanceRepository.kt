package de.tum.cit.cs.benchmarkservice.repository

import de.tum.cit.cs.benchmarkservice.model.Instance
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InstanceRepository : CoroutineCrudRepository<Instance, String> {

    @Query("{ '_id' : ?0 }")
    @Update("{ '\$set' : { 'tags' : ?1 } }")
    suspend fun updateTagsById(id: String, tags: List<String>): Int

}
