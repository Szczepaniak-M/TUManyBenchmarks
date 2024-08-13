package de.tum.cit.cs.benchmarkservice.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.tum.cit.cs.benchmarkservice.model.*
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class OutputParserService {
    private val mapper = jacksonObjectMapper()

    fun parseOutput(instance: Instance, benchmark: Benchmark, results: List<String>): BenchmarkResult {
        val typeRef = object : TypeReference<Map<String, Any>>() {}
        val parsedResults = mutableMapOf<String, Any>()
        for (result in results) {
            val resultsAsMap: Map<String, Any> = mapper.readValue(result, typeRef)
            parsedResults.putAll(resultsAsMap)
        }
        val time = ZonedDateTime.now().withSecond(0).toEpochSecond()
        return BenchmarkResult(
            instance.id!!,
            instance.name,
            benchmark.id,
            parsedResults,
            time
        )
    }
}
