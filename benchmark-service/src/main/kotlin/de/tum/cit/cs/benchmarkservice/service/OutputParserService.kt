package de.tum.cit.cs.benchmarkservice.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.tum.cit.cs.benchmarkservice.model.*
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class OutputParserService {
    private val mapper = jacksonObjectMapper()

    // TODO implement correct parsing
    fun parseOutput(instance: Instance, benchmark: Benchmark, results: List<Pair<NodeConfig, String>>): BenchmarkResult {
        val values = when (benchmark.configuration.outputType) {
            OutputType.SINGLE_NODE_SINGLE_VALUE -> {
                val parsedResult = mapper.readValue(results[0].second, BenchmarkSingleResult::class.java)
                mapOf("node-1" to parsedResult.value)
            }

            OutputType.SINGLE_NODE_MULTIPLE_VALUES -> {
                val parsedResult = mapper.readValue(results[0].second, BenchmarkListResult::class.java)
                mapOf("node-1" to parsedResult.values)
            }

            OutputType.MULTIPLE_NODES_SINGLE_VALUE -> {
                val output = mutableMapOf<String, Double>()
                for (result in results) {
                    val parsedResult = mapper.readValue(result.second, BenchmarkSingleResult::class.java)
                    output["node-${result.first.nodeId}"] = parsedResult.value
                }
                output
            }

            OutputType.MULTIPLE_NODES_MULTIPLE_VALUES -> {
                val output = mutableMapOf<String, List<Double>>()
                for (result in results) {
                    val parsedResult = mapper.readValue(result.second, BenchmarkListResult::class.java)
                    output["node-${result.first.nodeId}"] = parsedResult.values
                }
                output
            }
        }
        val time = ZonedDateTime.now().withMinute(0).toEpochSecond()
        return BenchmarkResult(
            instance.id!!,
            instance.name,
            benchmark.id,
            benchmark.configuration.outputType,
            values,
            time
        )
    }
}

data class BenchmarkSingleResult(
    val value: Double
)

data class BenchmarkListResult(
    val values: List<Double>
)
