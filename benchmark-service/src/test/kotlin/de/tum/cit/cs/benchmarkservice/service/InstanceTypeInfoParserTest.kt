package de.tum.cit.cs.benchmarkservice.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ec2.model.*


class InstanceTypeInfoParserTest {

    companion object {
        private const val INSTANCE_NAME = "t3.micro"
        private val V_CPU_INFO = VCpuInfo.builder()
            .defaultVCpus(8)
            .build()
        private val MEMORY_INFO = MemoryInfo.builder()
            .sizeInMiB(4096)
            .build()
        private val PROCESSOR_INFO = ProcessorInfo.builder()
            .supportedArchitectures(ArchitectureType.X86_64, ArchitectureType.I386)
            .build()
        private val INSTANCE_STORAGE_INFO = InstanceStorageInfo.builder()
            .disks(
                DiskInfo.builder()
                    .type("ssd")
                    .build()
            )
            .build()
        private val NETWORK_INFO = NetworkInfo.builder()
            .networkPerformance("Up to 25 Gigabit")
            .build()
    }

    private val parser = InstanceTypeInfoParser()

    @Test
    fun `parse instance name`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo.builder().instanceType(INSTANCE_NAME).build()

        // when
        val result = parser.parseInstanceName(instanceTypeInfo)

        // then
        assertEquals(INSTANCE_NAME, result)
    }

    @Test
    fun `parse instance tags`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo.builder()
            .instanceType(INSTANCE_NAME)
            .vCpuInfo(V_CPU_INFO)
            .memoryInfo(MEMORY_INFO)
            .processorInfo(PROCESSOR_INFO)
            .instanceStorageSupported(true)
            .instanceStorageInfo(INSTANCE_STORAGE_INFO)
            .networkInfo(NETWORK_INFO)
            .build()

        // when
        val result = parser.parseInstanceTags(instanceTypeInfo)

        // then
        val expectedTags = listOf("8 vCPUs", "4 GiB Memory", "x86_64", "i386", "SSD", "Up to 25 Gigabit Network")
        assertEquals(expectedTags, result)
    }

    @Test
    fun `do not parse instanceStorageInfo if instanceStorageSupported is false`() {
        // given
        val instanceTypeInfo = InstanceTypeInfo.builder()
            .instanceType(INSTANCE_NAME)
            .vCpuInfo(V_CPU_INFO)
            .memoryInfo(MEMORY_INFO)
            .processorInfo(PROCESSOR_INFO)
            .instanceStorageSupported(false)
            .instanceStorageInfo(INSTANCE_STORAGE_INFO)
            .networkInfo(NETWORK_INFO)
            .build()

        // when
        val result = parser.parseInstanceTags(instanceTypeInfo)

        // then
        val expectedTags = listOf("8 vCPUs", "4 GiB Memory", "x86_64", "i386", "Up to 25 Gigabit Network")
        assertEquals(expectedTags, result)
    }
}
