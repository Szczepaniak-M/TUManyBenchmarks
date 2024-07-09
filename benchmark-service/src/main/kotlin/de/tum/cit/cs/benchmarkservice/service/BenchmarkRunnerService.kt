package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.BenchmarkResult
import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import de.tum.cit.cs.benchmarkservice.model.InstanceWithBenchmarks
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import java.util.*

@Service
class BenchmarkRunnerService(
    private val ec2ConfigurationService: Ec2ConfigurationService,
    private val awsService: AwsService,
    private val sshService: SshService,
    private val outputParserService: OutputParserService
) {
    private val logger = KotlinLogging.logger {}

    suspend fun runBenchmarksForInstance(instanceWithBenchmarks: InstanceWithBenchmarks): List<BenchmarkResult> {
        val instance = instanceWithBenchmarks.instance
        var benchmarksResult = listOf<BenchmarkResult>()
        coroutineScope {
            benchmarksResult = instanceWithBenchmarks.benchmarks.map { benchmark ->
                val benchmarkRunId = UUID.randomUUID().toString()
                logger.info { "Benchmark ${benchmarkRunId}: Starting benchmark '${benchmark.configuration.name}' for instance '${instance.name}'" }
                async {
                    val ec2Configuration =
                        ec2ConfigurationService.generateEc2Configuration(instance, benchmark, benchmarkRunId)
                    var benchmarkResult: BenchmarkResult? = null
                    try {
                        createAwsResources(ec2Configuration)
                        val results = sshService.executeBenchmark(ec2Configuration)
                        benchmarkResult = outputParserService.parseOutput(instance, benchmark, results)
                    } catch (e: Exception) {
                        logger.error {
                            "Benchmark ${benchmarkRunId}: Stopping benchmark '${benchmark.configuration.name}'" +
                                    " for instance '${instance.name}' due to error: ${e.message}"
                        }
                    } finally {
                        deleteAwsResources(ec2Configuration)
                    }
                    benchmarkResult
                }
            }
                .awaitAll()
                .filterNotNull()
        }
        return benchmarksResult
    }

    private suspend fun createAwsResources(ec2Configuration: Ec2Configuration) {
        awsService.createVpc(ec2Configuration)
        awsService.createInternetGateway(ec2Configuration)
        awsService.configureRouteTable(ec2Configuration)
        awsService.createSubnet(ec2Configuration)
        awsService.createSecurityGroup(ec2Configuration)
        awsService.startEc2Instance(ec2Configuration)
        awsService.getEc2InstanceAddresses(ec2Configuration)
    }

    private suspend fun deleteAwsResources(ec2Configuration: Ec2Configuration) {
        val instances = awsService.terminateEc2Instance(ec2Configuration)
        awsService.deleteSecurityGroup(ec2Configuration, instances)
        awsService.deleteSubnet(ec2Configuration)
        awsService.deleteInternetGateway(ec2Configuration)
        awsService.deleteVpc(ec2Configuration)
    }
}
