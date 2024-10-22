package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.collections.listOf

class InstanceTypeInfoParserTest {

    companion object {
        private val INSTANCE_NAME = InstanceType.T3Micro
        private const val INSTANCE_NAME_STRING = "t3.micro"
        private val V_CPU_INFO = VCpuInfo { defaultVCpus = 8 }
        private val MEMORY_INFO = MemoryInfo { sizeInMib = 4096 }
        private val PROCESSOR_INFO = ProcessorInfo { supportedArchitectures = listOf(ArchitectureType.X86_64, ArchitectureType.I386) }
        private val DISK_INFO_1 = DiskInfo {
            count = 2
            type = DiskType.Ssd
            sizeInGb = 100
        }
        private val DISK_INFO_2 = DiskInfo {
            count = 4
            type = DiskType.Ssd
            sizeInGb = 200
        }
        private val DISK_INFO_3 = DiskInfo {
            count = 1
            type = DiskType.Ssd
            sizeInGb = 300
        }
        private val INSTANCE_STORAGE_INFO_1 = InstanceStorageInfo {
            totalSizeInGb = 1000
            disks = listOf(DISK_INFO_1, DISK_INFO_2)
            nvmeSupport = EphemeralNvmeSupport.Supported
        }
        private val INSTANCE_STORAGE_INFO_2 = InstanceStorageInfo {
            totalSizeInGb = 100
            disks = listOf(DISK_INFO_3)
            nvmeSupport = EphemeralNvmeSupport.Unsupported
        }
        private val NETWORK_INFO = NetworkInfo { networkPerformance = "Up to 25 Gigabit" }
        private val HYPERVISOR = InstanceTypeHypervisor.Nitro
    }

    private val parser = InstanceTypeInfoParser()

    @Test
    fun `parse instance type info`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo {
            instanceType = INSTANCE_NAME
            vCpuInfo = V_CPU_INFO
            memoryInfo = MEMORY_INFO
            processorInfo = PROCESSOR_INFO
            instanceStorageSupported = true
            instanceStorageInfo = INSTANCE_STORAGE_INFO_1
            networkInfo = NETWORK_INFO
            hypervisor = HYPERVISOR
            bareMetal = false
            currentGeneration = false
        }
        val expectedTags = listOf(
            "Family t3", "8 vCPUs", "4 GiB Memory", "Up to 25 Gigabit Network",
            "x86-64", "i386", "SSD", "Hypervisor Nitro", "Previous Generation"
        )

        // when
        val result = parser.parse(instanceTypeInfo)

        // then
        assertEquals(INSTANCE_NAME_STRING, result.name)
        assertEquals(8, result.vCpu)
        assertEquals(BigDecimal(4), result.memory)
        assertEquals("Up to 25 Gigabit", result.network)
        assertEquals("1000 GB (2 * 100 GB NVMe SSD + 4 * 200 GB NVMe SSD)", result.storage)
        assertEquals(expectedTags, result.tags)
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
            instanceStorageInfo = null
            networkInfo = NETWORK_INFO
        }
        val expectedTags = listOf("Family t3", "8 vCPUs", "4 GiB Memory", "Up to 25 Gigabit Network", "x86-64", "i386")

        // when
        val result = parser.parse(instanceTypeInfo)

        // then
        assertEquals(expectedTags, result.tags)
        assertEquals("EBS only", result.storage)
    }

    @Test
    fun `do not add NVMe if NVMe is not supported`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo {
            instanceType = INSTANCE_NAME
            vCpuInfo = V_CPU_INFO
            memoryInfo = MEMORY_INFO
            processorInfo = PROCESSOR_INFO
            instanceStorageSupported = true
            instanceStorageInfo = INSTANCE_STORAGE_INFO_2
            networkInfo = NETWORK_INFO
        }
        val expectedTags = listOf(
            "Family t3", "8 vCPUs", "4 GiB Memory",
            "Up to 25 Gigabit Network", "x86-64", "i386", "SSD"
        )

        // when
        val result = parser.parse(instanceTypeInfo)

        // then
        assertEquals(expectedTags, result.tags)
        assertEquals("300 GB SSD", result.storage)
    }

    @Test
    fun `do not parse hypervisor if its null`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo {
            instanceType = INSTANCE_NAME
            vCpuInfo = V_CPU_INFO
            memoryInfo = MEMORY_INFO
            processorInfo = PROCESSOR_INFO
            instanceStorageSupported = false
            instanceStorageInfo = INSTANCE_STORAGE_INFO_1
            networkInfo = NETWORK_INFO
            hypervisor = null
            bareMetal = true
        }
        val expectedTags = listOf(
            "Family t3", "8 vCPUs", "4 GiB Memory", "Up to 25 Gigabit Network",
            "x86-64", "i386", "Bare Metal"
        )

        // when
        val result = parser.parse(instanceTypeInfo)

        // then
        assertEquals(expectedTags, result.tags)
    }

    @Test
    fun `do parse name when it is unknown type by SDK`() {
        // given
        val instanceName = "t12.micro"
        val instanceTypeInfo = InstanceTypeInfo {
            instanceType = InstanceType.fromValue(instanceName)
            vCpuInfo = V_CPU_INFO
            memoryInfo = MEMORY_INFO
            processorInfo = PROCESSOR_INFO
            instanceStorageSupported = false
            instanceStorageInfo = INSTANCE_STORAGE_INFO_1
            networkInfo = NETWORK_INFO
        }
        val expectedTags = listOf("Family t12", "8 vCPUs", "4 GiB Memory", "Up to 25 Gigabit Network", "x86-64", "i386")

        // when
        val result = parser.parse(instanceTypeInfo)

        // then
        assertEquals(instanceName, result.name)
        assertEquals(expectedTags, result.tags)
    }
}
