package de.tum.cit.cs.webpage.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "benchmark-details")
data class BenchmarkDetails(
    @Id
    val id: String,
    val name: String,
    val description: String,
    val instanceTypes: List<String> = emptyList(),
    val instanceTags: List<List<String>> = emptyList(),
    val seriesX: List<String> = emptyList(),
    val seriesY: List<String> = emptyList(),
    val seriesOther: List<String> = emptyList(),
)
