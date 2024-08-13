package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.model.InstanceTypeInfo
import de.tum.cit.cs.benchmarkservice.model.Instance
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class InstanceTypeInfoParser {

    fun parse(instanceTypeInfo: InstanceTypeInfo): Instance {
        val name = parseInstanceName(instanceTypeInfo)
        val vCpu = parseCpu(instanceTypeInfo)
        val memory = parseMemory(instanceTypeInfo)
        val network = parseNetwork(instanceTypeInfo)
        val tags = parseInstanceTags(instanceTypeInfo)
        return Instance(null, name, vCpu, memory, network, tags)
    }

    private fun parseInstanceName(instanceTypeInfo: InstanceTypeInfo): String {
        return instanceTypeInfo.instanceType?.value ?: "UNKNOWN"
    }

    private fun parseCpu(instanceTypeInfo: InstanceTypeInfo): Int {
        return instanceTypeInfo.vCpuInfo?.defaultVCpus ?: 0
    }

    private fun parseMemory(instanceTypeInfo: InstanceTypeInfo): BigDecimal {
        val memory = instanceTypeInfo.memoryInfo?.sizeInMib?.div(1024.0) ?: 0.0
        return BigDecimal(memory)
            .setScale(3, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
    }

    private fun parseNetwork(instanceTypeInfo: InstanceTypeInfo): String {
        return instanceTypeInfo.networkInfo?.networkPerformance ?: ""
    }

    private fun parseInstanceTags(instanceTypeInfo: InstanceTypeInfo): List<String> {
        val tags = mutableListOf<String>()
        tags.add(parseFamily(instanceTypeInfo))
        tags.add(parseCpuTag(instanceTypeInfo))
        tags.add(parseMemoryTag(instanceTypeInfo))
        tags.add(parseNetworkTag(instanceTypeInfo))
        tags.addAll(parseArchitectures(instanceTypeInfo))
        tags.addAll(parseStorageInfo(instanceTypeInfo))
        parseHypervisor(instanceTypeInfo)?.let { tags.add(it) }
        parseMetal(instanceTypeInfo)?.let { tags.add(it) }
        parsePreviousGeneration(instanceTypeInfo)?.let { tags.add(it) }
        return tags
    }

    private fun parseFamily(instanceTypeInfo: InstanceTypeInfo): String {
        val family = instanceTypeInfo.instanceType?.value?.split(".")?.get(0)
        return "Family $family"
    }

    private fun parseCpuTag(instanceTypeInfo: InstanceTypeInfo): String {
        val vCpu = parseCpu(instanceTypeInfo)
        return "$vCpu vCPUs"
    }

    private fun parseMemoryTag(instanceTypeInfo: InstanceTypeInfo): String {
        val memoryDecimal = parseMemory(instanceTypeInfo)
        return "$memoryDecimal GiB Memory"
    }

    private fun parseNetworkTag(instanceTypeInfo: InstanceTypeInfo): String {
        val networkSpeed = parseNetwork(instanceTypeInfo)
        return "$networkSpeed Network"
    }

    private fun parseArchitectures(instanceTypeInfo: InstanceTypeInfo): List<String> {
        return instanceTypeInfo.processorInfo
            ?.supportedArchitectures
            ?.map { it.value.replace("_", "-").replace("arm", "ARM") }
            ?: emptyList()
    }

    private fun parseStorageInfo(instanceTypeInfo: InstanceTypeInfo): List<String> {
        val tags = mutableListOf<String>()
        if (instanceTypeInfo.instanceStorageSupported == true) {
            val disk = instanceTypeInfo.instanceStorageInfo?.disks?.get(0)
            disk?.type?.let { tags.add(it.value.uppercase()) }
        }
        return tags
    }

    private fun parseHypervisor(instanceTypeInfo: InstanceTypeInfo): String? {
        val hypervisor = instanceTypeInfo.hypervisor?.toString()
        return if (hypervisor != null) {
            "Hypervisor $hypervisor"
        } else null
    }

    private fun parseMetal(instanceTypeInfo: InstanceTypeInfo): String? {
        val isMetal = instanceTypeInfo.bareMetal
        return if (isMetal == true) {
            "Bare Metal"
        } else null
    }

    private fun parsePreviousGeneration(instanceTypeInfo: InstanceTypeInfo): String? {
        val isCurrentGeneration = instanceTypeInfo.currentGeneration
        return if (isCurrentGeneration == false) {
            "Previous Generation"
        } else null
    }
}
