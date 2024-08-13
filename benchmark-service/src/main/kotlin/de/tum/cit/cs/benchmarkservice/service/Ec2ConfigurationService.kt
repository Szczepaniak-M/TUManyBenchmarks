package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.*
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class Ec2ConfigurationService(
    private val instanceRepository: InstanceRepository
) {

    @Value("\${aws.ec2.default-ami.x86_64}")
    lateinit var amiX86_64: String

    @Value("\${aws.ec2.default-ami.arm}")
    lateinit var amiArm: String

    suspend fun generateEc2Configuration(instance: Instance, benchmark: Benchmark, benchmarkRunId: String): Ec2Configuration {
        val defaultBenchmarkConfiguration = benchmark.nodes.filter { it.nodeId == 0 }.getOrNull(0)
        val nodesBenchmarkConfiguration = benchmark.nodes.filter { it.nodeId != 0 }
        val defaultInstanceType = instance.name
        val ec2NodeConfigurations = mutableListOf<NodeConfig>()

        for (node in nodesBenchmarkConfiguration) {
            val nodeConfig = NodeConfig(
                nodeId = node.nodeId,
                instanceType = node.instanceType ?: defaultInstanceType,
                image = node.image ?: defaultBenchmarkConfiguration?.image ?: getDefaultImage(node, instance),
                ansibleConfiguration = node.ansibleConfiguration ?: defaultBenchmarkConfiguration?.ansibleConfiguration,
                benchmarkCommand = node.benchmarkCommand ?: defaultBenchmarkConfiguration?.benchmarkCommand,
                outputCommand = node.outputCommand ?: defaultBenchmarkConfiguration?.outputCommand
            )
            ec2NodeConfigurations.add(nodeConfig)
        }

        if (ec2NodeConfigurations.size < benchmark.configuration.instanceNumber) {
            for (i in ec2NodeConfigurations.size + 1..benchmark.configuration.instanceNumber) {
                val nodeConfig = NodeConfig(
                    nodeId = i,
                    instanceType = defaultInstanceType,
                    image = defaultBenchmarkConfiguration?.image ?: getDefaultImage(null, instance),
                    ansibleConfiguration = defaultBenchmarkConfiguration?.ansibleConfiguration,
                    benchmarkCommand = defaultBenchmarkConfiguration?.benchmarkCommand,
                    outputCommand = defaultBenchmarkConfiguration?.outputCommand
                )
                ec2NodeConfigurations.add(nodeConfig)
            }
        }
        return Ec2Configuration(benchmarkRunId, benchmark.configuration.directory, ec2NodeConfigurations)
    }

    private suspend fun getDefaultImage(node: Node?, instance: Instance): String {
        return if (node?.instanceType != null && node.instanceType != instance.name && node.image == null) {
            val tempInstance = instanceRepository.findInstanceByName(node.instanceType)
            if (tempInstance.tags.contains("ARM64")) {
                amiArm
            } else {
                amiX86_64
            }
        } else if (instance.tags.contains("ARM64")) {
            amiArm
        } else {
            amiX86_64
        }
    }
}
