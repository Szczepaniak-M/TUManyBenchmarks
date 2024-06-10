package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class InstanceTypeInfoParserTest {

    companion object {
        private val INSTANCE_NAME = InstanceType.T3Micro
        private const val INSTANCE_NAME_STRING = "t3.micro"
        private val V_CPU_INFO = VCpuInfo { defaultVCpus = 8 }
        private val MEMORY_INFO = MemoryInfo { sizeInMib = 4096 }
        private val PROCESSOR_INFO = ProcessorInfo { supportedArchitectures = listOf(ArchitectureType.X86_64, ArchitectureType.I386) }
        private val INSTANCE_STORAGE_INFO = InstanceStorageInfo { disks = listOf(DiskInfo { type = DiskType.Ssd }) }
        private val NETWORK_INFO = NetworkInfo { networkPerformance = "Up to 25 Gigabit" }
    }

    private val parser = InstanceTypeInfoParser()

    @Test
    fun `parse instance name`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo { instanceType = INSTANCE_NAME }

        // when
        val result = parser.parseInstanceName(instanceTypeInfo)

        // then
        assertEquals(INSTANCE_NAME_STRING, result)
    }

    @Test
    fun `parse instance tags`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo {
            instanceType = INSTANCE_NAME
            vCpuInfo = V_CPU_INFO
            memoryInfo = MEMORY_INFO
            processorInfo = PROCESSOR_INFO
            instanceStorageSupported = true
            instanceStorageInfo = INSTANCE_STORAGE_INFO
            networkInfo = NETWORK_INFO
        }

        // when
        val result = parser.parseInstanceTags(instanceTypeInfo)

        // then
        val expectedTags = listOf("8 vCPUs", "4 GiB Memory", "x86_64", "i386", "ssd", "Up to 25 Gigabit Network")
        assertEquals(expectedTags, result)
    }

    @Test
    fun `do not parse instanceStorageInfo if instanceStorageSupported is false`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo {
            instanceType = INSTANCE_NAME
            vCpuInfo = V_CPU_INFO
            memoryInfo = MEMORY_INFO
            processorInfo = PROCESSOR_INFO
            instanceStorageSupported = false
            instanceStorageInfo = INSTANCE_STORAGE_INFO
            networkInfo = NETWORK_INFO
        }

        // when
        val result = parser.parseInstanceTags(instanceTypeInfo)

        // then
        val expectedTags = listOf("8 vCPUs", "4 GiB Memory", "x86_64", "i386", "Up to 25 Gigabit Network")
        assertEquals(expectedTags, result)
    }
}
