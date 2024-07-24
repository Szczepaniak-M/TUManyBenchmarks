package de.tum.cit.cs.benchmarkservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "benchmarks")
data class Benchmark(
    @Id
    val id: String,
    val configuration: Configuration,
    val nodes: List<Node>,
)

data class Configuration(
    val name: String,
    val description: String,
    val directory: String,
    val cron: String,
    val instanceNumber: Int,
    val instanceTags: List<List<String>>?,
    val instanceTypes: List<String>?
)

data class Node(
    val nodeId: Int,
    val instanceType: String?,
    val image: String?,
    val ansibleConfiguration: String?,
    val benchmarkCommand: String?,
    val outputCommand: String?
)
