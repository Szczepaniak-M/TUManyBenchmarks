package de.tum.cit.cs.benchmarkservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "instances")
data class Instance(
    @Id
    val id: String,
    val name: String,
    val tags: List<String>
)
