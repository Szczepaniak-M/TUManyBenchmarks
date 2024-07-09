package de.tum.cit.cs.benchmarkservice.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "benchmarks")
data class BenchmarkCron(
    @Id
    val id: String,

    @Field("configuration.cron")
    val cron: String,
)
