package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class BenchmarkRunnerService(
    val awsService: AwsService,
    val gitHubService: GitHubService,
    val sshService: SshService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun runBenchmarksForInstance(instanceWithBenchmarks: InstanceWithBenchmarks): List<BenchmarkResult> {
        val instance = instanceWithBenchmarks.instance
        var benchmarksResult: List<BenchmarkResult> = listOf()
        runBlocking {
            benchmarksResult = instanceWithBenchmarks.benchmarks
                .map { benchmark ->
                    logger.info { "Starting benchmark ${benchmark.configuration.name} for instance ${instance.name}" }
                    async {
                        var vpcId: String? = null
                        var subnetId: String? = null
                        var securityGroupId: String? = null
                        var instances: List<String>? = null
                        var benchmarkResult: BenchmarkResult? = null
                        try {
                            vpcId = awsService.createVpc()
                            subnetId = awsService.createSubnet(vpcId)
                            securityGroupId = awsService.createSecurityGroup(vpcId)
                            val instanceCount = benchmark.configuration.instanceNumber
                            instances = awsService.startEc2Instance(instance, instanceCount, subnetId, securityGroupId)
                            val instanceAddresses = awsService.getEc2InstanceAddresses(instances)

                            val curls = gitHubService.getCurlsForFilesFromDirectory(benchmark.configuration.directory)
                            val results = sshService.executeBenchmark(benchmark, instanceAddresses, curls)

                            benchmarkResult = parseOutput(instance, benchmark, results)
                        } catch (e: Exception) {
                            logger.error { "Stopping benchmark ${benchmark.configuration.name} for instance ${instance.name} due to error: ${e.message}" }
                        } finally {
                            if (instances != null) {
                                awsService.terminateEc2Instance(instances)
                            }
                            if (securityGroupId != null) {
                                awsService.deleteSecurityGroup(securityGroupId)
                            }
                            if (subnetId != null) {
                                awsService.deleteSubnet(subnetId)
                            }

                            if (vpcId != null) {
                                awsService.deleteVpc(vpcId)
                            }
                        }
                        benchmarkResult
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
        return benchmarksResult
    }

    // TODO implement correct parsing
    fun parseOutput(instance: Instance, benchmark: Benchmark, results: List<String>): BenchmarkResult {
        val values = when (benchmark.configuration.outputType) {
            OutputType.SINGLE_NODE_SINGLE_VALUE -> {
                mapOf(Pair("value", results[0]))
            }

            OutputType.SINGLE_NODE_MULTIPLE_VALUES -> {
                mapOf(Pair("value", results[0]))
            }

            OutputType.MULTIPLE_NODES_SINGLE_VALUE -> {
                val output = mutableMapOf<String, String>()
                for ((counter, result) in results.withIndex()) {
                    output["value-$counter"] = result
                }
                output
            }

            OutputType.MULTIPLE_NODES_MULTIPLE_VALUES -> {
                val output = mutableMapOf<String, String>()
                for ((counter, result) in results.withIndex()) {
                    output["value-$counter"] = result
                }
                output
            }
        }
        val time = ZonedDateTime.now().withMinute(0).toEpochSecond()
        return BenchmarkResult(
            instance.id!!,
            benchmark.id,
            benchmark.configuration.outputType,
            values,
            time
        )
    }
}
