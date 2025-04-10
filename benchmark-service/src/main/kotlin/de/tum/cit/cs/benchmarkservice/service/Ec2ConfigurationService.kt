package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.*
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class Ec2ConfigurationService(
    private val instanceRepository: InstanceRepository,
    @Value("\${aws.ec2.default-ami.x86}")
    private val amiX86: String,
    @Value("\${aws.ec2.default-ami.arm}")
    private val amiArm: String
) {

    suspend fun generateEc2Configuration(instance: Instance, benchmark: Benchmark, benchmarkRunId: String): Ec2Configuration {
        val defaultNodeConfiguration = benchmark.nodes.filter { it.nodeId == 0 }.getOrNull(0)
        val nodesBenchmarkConfiguration = benchmark.nodes.filter { it.nodeId != 0 }
        val defaultInstanceType = instance.name
        val ec2NodeConfigurations = mutableListOf<NodeConfig>()
        var vcpuCost = 0

        for (node in nodesBenchmarkConfiguration) {
            var otherInstance: Instance? = null
            if (node.instanceType != null && node.instanceType != instance.name) {
                otherInstance = instanceRepository.findInstanceByName(node.instanceType)
            }
            val nodeConfig = NodeConfig(
                nodeId = node.nodeId,
                instanceType = node.instanceType ?: defaultInstanceType,
                image = getImage(node, instance, otherInstance, defaultNodeConfiguration),
                ansibleConfiguration = node.ansibleConfiguration ?: defaultNodeConfiguration?.ansibleConfiguration,
                benchmarkCommand = node.benchmarkCommand ?: defaultNodeConfiguration?.benchmarkCommand,
                outputCommand = node.outputCommand ?: defaultNodeConfiguration?.outputCommand
            )
            ec2NodeConfigurations.add(nodeConfig)
            vcpuCost += getCpuCost(instance, otherInstance)
        }

        if (ec2NodeConfigurations.size < benchmark.configuration.instanceNumber) {
            for (i in ec2NodeConfigurations.size + 1..benchmark.configuration.instanceNumber) {
                val nodeConfig = NodeConfig(
                    nodeId = i,
                    instanceType = defaultInstanceType,
                    image = getImage(null, instance, null, defaultNodeConfiguration),
                    ansibleConfiguration = defaultNodeConfiguration?.ansibleConfiguration,
                    benchmarkCommand = defaultNodeConfiguration?.benchmarkCommand,
                    outputCommand = defaultNodeConfiguration?.outputCommand
                )
                ec2NodeConfigurations.add(nodeConfig)
                vcpuCost += getCpuCost(instance, null)
            }
        }
        return Ec2Configuration(benchmarkRunId, benchmark.configuration.directory, vcpuCost, ec2NodeConfigurations)
    }

    private suspend fun getImage(node: Node?, instance: Instance, otherInstance: Instance?, defaultNodeConfiguration: Node?): String {
        val isArm = if (otherInstance != null) {
            otherInstance.tags.contains("ARM64")
        } else {
            instance.tags.contains("ARM64")
        }

        return if (isArm) {
            return node?.imageArm ?: defaultNodeConfiguration?.imageArm ?: amiArm
        } else {
            return node?.imageX86 ?: defaultNodeConfiguration?.imageX86 ?: amiX86
        }
    }

    private suspend fun getCpuCost(instance: Instance, otherInstance: Instance?): Int {
        return if (otherInstance != null) {
            otherInstance.vCpu
        } else {
            instance.vCpu
        }
    }
}
