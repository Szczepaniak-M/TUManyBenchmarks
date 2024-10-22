package de.tum.cit.cs.benchmarkservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "instances")
data class Instance(
    @Id
    val id: String?,
    val name: String,
    val vCpu: Int,
    val memory: BigDecimal,
    val network: String,
    val storage: String,
    val tags: List<String>
)
