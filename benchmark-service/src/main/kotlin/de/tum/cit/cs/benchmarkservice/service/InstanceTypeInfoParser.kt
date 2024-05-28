package de.tum.cit.cs.benchmarkservice.service

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class InstanceTypeInfoParser {

    fun parseInstanceName(instanceTypeInfo: InstanceTypeInfo): String {
        return instanceTypeInfo.instanceTypeAsString()
    }

    // TODO discuss additional tags
    fun parseInstanceTags(instanceTypeInfo: InstanceTypeInfo): List<String> {
        val tags = mutableListOf<String>()
        tags.add(parseCPUs(instanceTypeInfo))
        tags.add(parseMemory(instanceTypeInfo))
        tags.addAll(parseArchitectures(instanceTypeInfo))
        tags.addAll(parseStorageInfo(instanceTypeInfo))
        tags.add(parseNetwork(instanceTypeInfo))
        return tags
    }

    private fun parseCPUs(instanceTypeInfo: InstanceTypeInfo): String {
        val vCpu = instanceTypeInfo.vCpuInfo().defaultVCpus()
        return "$vCpu vCPUs"
    }

    private fun parseMemory(instanceTypeInfo: InstanceTypeInfo): String {
        val memory = instanceTypeInfo.memoryInfo().sizeInMiB() / 1024.0
        val memoryDecimal = BigDecimal(memory)
            .setScale(3, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()
        return "$memoryDecimal GiB Memory"
    }

    private fun parseArchitectures(instanceTypeInfo: InstanceTypeInfo): List<String> {
        return instanceTypeInfo.processorInfo()
            .supportedArchitectures()
            .map { it.toString() }
    }

    private fun parseStorageInfo(instanceTypeInfo: InstanceTypeInfo): List<String> {
        val tags = mutableListOf<String>()
        if (instanceTypeInfo.instanceStorageSupported()) {
            val disk = instanceTypeInfo.instanceStorageInfo().disks()[0]
            tags.add(disk.type().name)
        }
        return tags
    }

    private fun parseNetwork(instanceTypeInfo: InstanceTypeInfo): String {
        val networkSpeed = instanceTypeInfo.networkInfo().networkPerformance()
        return "$networkSpeed Network"
    }
}
