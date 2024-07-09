package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.model.InstanceTypeInfo
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class InstanceTypeInfoParser {

    fun parseInstanceName(instanceTypeInfo: InstanceTypeInfo): String {
        return instanceTypeInfo.instanceType?.value ?: "UNKNOWN"
    }

    fun parseInstanceTags(instanceTypeInfo: InstanceTypeInfo): List<String> {
        val tags = mutableListOf<String>()
        tags.add(parseCPUs(instanceTypeInfo))
        tags.add(parseMemory(instanceTypeInfo))
        tags.addAll(parseArchitectures(instanceTypeInfo))
        tags.addAll(parseStorageInfo(instanceTypeInfo))
        tags.add(parseNetwork(instanceTypeInfo))
        parseHypervisor(instanceTypeInfo)?.let { tags.add(it) }
        parseMetal(instanceTypeInfo)?.let { tags.add(it) }
        parsePreviousGeneration(instanceTypeInfo)?.let { tags.add(it) }
        return tags
    }

    private fun parseCPUs(instanceTypeInfo: InstanceTypeInfo): String {
        val vCpu = instanceTypeInfo.vCpuInfo?.defaultVCpus
        return "$vCpu vCPUs"
    }

    private fun parseMemory(instanceTypeInfo: InstanceTypeInfo): String {
        val memory = instanceTypeInfo.memoryInfo?.sizeInMib?.div(1024.0) ?: 0.0
        val memoryDecimal = BigDecimal(memory)
            .setScale(3, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()
        return "$memoryDecimal GiB Memory"
    }

    private fun parseArchitectures(instanceTypeInfo: InstanceTypeInfo): List<String> {
        return instanceTypeInfo.processorInfo
            ?.supportedArchitectures
            ?.map { it.value } ?: emptyList()
    }

    private fun parseStorageInfo(instanceTypeInfo: InstanceTypeInfo): List<String> {
        val tags = mutableListOf<String>()
        if (instanceTypeInfo.instanceStorageSupported == true) {
            val disk = instanceTypeInfo.instanceStorageInfo?.disks?.get(0)
            disk?.type?.let { tags.add(it.value) }
        }
        return tags
    }

    private fun parseNetwork(instanceTypeInfo: InstanceTypeInfo): String {
        val networkSpeed = instanceTypeInfo.networkInfo?.networkPerformance
        return "$networkSpeed Network"
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
