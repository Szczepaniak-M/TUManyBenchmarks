package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.model.*
import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class InstanceUpdateSchedulerServiceTest {

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @MockkBean
    private lateinit var awsService: AwsService

    @MockkBean
    private lateinit var parser: InstanceTypeInfoParser

    companion object {
        private const val ID_1 = "ID1"
        private const val ID_2 = "ID2"
        private const val INSTANCE_NAME_1_STRING = "t3.micro"
        private const val INSTANCE_NAME_2_STRING = "t3.small"
        private val INSTANCE_NAME_1 = InstanceType.T3Micro
        private val V_CPU_INFO = VCpuInfo { defaultVCpus = 8 }
        private val MEMORY_INFO = MemoryInfo { sizeInMib = 4096 }
        private val PROCESSOR_INFO = ProcessorInfo { supportedArchitectures = listOf(ArchitectureType.X86_64, ArchitectureType.I386) }
        private val NETWORK_INFO = NetworkInfo { networkPerformance = "Up to 25 Gigabit" }
        private val INSTANCE_TYPE_INFO_1 = InstanceTypeInfo {
            instanceType = INSTANCE_NAME_1
            vCpuInfo = V_CPU_INFO
            memoryInfo = MEMORY_INFO
            processorInfo = PROCESSOR_INFO
            instanceStorageSupported = true
            networkInfo = NETWORK_INFO
        }
        private val TAG_LISTS_1 = listOf("8 vCPUs", "4 GiB Memory", "x86_64", "i386", "Up to 25 Gigabit Network")
        private val TAG_LISTS_2 = listOf("8 vCPUs", "4 GiB Memory", "x86_64", "i386")
        private val INSTANCE_1 = Instance(null, INSTANCE_NAME_1_STRING, TAG_LISTS_1)
        private val INSTANCE_2 = Instance(ID_2, INSTANCE_NAME_2_STRING, TAG_LISTS_2)
        private val INSTANCE_1_OUT_OF_DATE = Instance(ID_1, INSTANCE_NAME_1_STRING, TAG_LISTS_2)
        private val INSTANCE_1_WITH_ID = Instance(ID_1, INSTANCE_NAME_1_STRING, TAG_LISTS_1)
    }

    @Test
    fun `save instance if not in database`() = runTest {
        // given
        val service = InstanceUpdateSchedulerService(instanceRepository, awsService, parser)
        coEvery { awsService.getInstancesFromAws() } returns listOf(INSTANCE_TYPE_INFO_1)
        coEvery { instanceRepository.findAll() } returns flowOf(INSTANCE_2)
        coEvery { instanceRepository.save(INSTANCE_1) } returns INSTANCE_1
        every { parser.parseInstanceName(INSTANCE_TYPE_INFO_1) } returns INSTANCE_NAME_1_STRING
        every { parser.parseInstanceTags(INSTANCE_TYPE_INFO_1) } returns TAG_LISTS_1

        // when
        service.updateInstances()

        // then
        coVerify { awsService.getInstancesFromAws() }
        coVerify { instanceRepository.findAll() }
        coVerify { instanceRepository.save(INSTANCE_1) }
        verify { parser.parseInstanceName(INSTANCE_TYPE_INFO_1) }
        verify { parser.parseInstanceTags(INSTANCE_TYPE_INFO_1) }
        coVerify(exactly = 0) { instanceRepository.updateTagsById(any(), any()) }
    }

    @Test
    fun `update instance if in database is out-of-date`() = runTest {
        // given
        val service = InstanceUpdateSchedulerService(instanceRepository, awsService, parser)
        coEvery { awsService.getInstancesFromAws() } returns listOf(INSTANCE_TYPE_INFO_1)
        coEvery { instanceRepository.findAll() } returns flowOf(INSTANCE_1_OUT_OF_DATE)
        coEvery { instanceRepository.updateTagsById(ID_1, TAG_LISTS_1) } returns 1
        every { parser.parseInstanceName(INSTANCE_TYPE_INFO_1) } returns INSTANCE_NAME_1_STRING
        every { parser.parseInstanceTags(INSTANCE_TYPE_INFO_1) } returns TAG_LISTS_1

        // when
        service.updateInstances()

        // then
        coVerify { awsService.getInstancesFromAws() }
        coVerify { instanceRepository.findAll() }
        coVerify { instanceRepository.updateTagsById(ID_1, TAG_LISTS_1) }
        verify { parser.parseInstanceName(INSTANCE_TYPE_INFO_1) }
        verify { parser.parseInstanceTags(INSTANCE_TYPE_INFO_1) }
        coVerify(exactly = 0) { instanceRepository.save(any()) }
    }

    @Test
    fun `not update instance if in database is up-to-date`() = runTest {
        // given
        val service = InstanceUpdateSchedulerService(instanceRepository, awsService, parser)
        coEvery { awsService.getInstancesFromAws() } returns listOf(INSTANCE_TYPE_INFO_1)
        coEvery { instanceRepository.findAll() } returns flowOf(INSTANCE_1_WITH_ID)
        every { parser.parseInstanceName(INSTANCE_TYPE_INFO_1) } returns INSTANCE_NAME_1_STRING
        every { parser.parseInstanceTags(INSTANCE_TYPE_INFO_1) } returns TAG_LISTS_1

        // when
        service.updateInstances()

        // then
        coVerify { awsService.getInstancesFromAws() }
        coVerify { instanceRepository.findAll() }
        verify { parser.parseInstanceName(INSTANCE_TYPE_INFO_1) }
        verify { parser.parseInstanceTags(INSTANCE_TYPE_INFO_1) }
        coVerify(exactly = 0) { instanceRepository.updateTagsById(any(), any()) }
        coVerify(exactly = 0) { instanceRepository.save(any()) }
    }
}
