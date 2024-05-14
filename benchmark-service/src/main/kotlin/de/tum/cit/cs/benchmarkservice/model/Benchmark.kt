package de.tum.cit.cs.benchmarkservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "benchmarks")
data class Benchmark(
    @Id
    val id: String,
    val metadata: Metadata,
    val nodes: List<Node>,
)

data class Metadata(
    val name: String,
    val description: String,
    val cron: String,
    val outputType: OutputType,
    val instanceNumber: Int,
    val instanceTags: List<List<String>>?,
    val instanceType: List<String>
)

data class Node(
    val id: Int,
    val ansibleConfiguration: String,
    val benchmarkCommand: String,
    val outputCommand: String
)

