package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.model.*
import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.Configuration
import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.model.OutputType
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class InstanceServiceTest {

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @MockkBean
    private lateinit var awsService: AwsService

    @MockkBean
    private lateinit var parser: InstanceTypeInfoParser

    companion object {
        private val TEMPLATE_INSTANCE = Instance(id = "id", name = "t2.micro", tags = emptyList())
        private val TEMPLATE_CONFIGURATION = Configuration(
            name = "", description = "", cron = "",
            outputType = OutputType.SINGLE_VALUE, instanceNumber = 0,
            instanceTags = null, instanceType = null
        )
        private val TEMPLATE_BENCHMARK = Benchmark(id = "", configuration = TEMPLATE_CONFIGURATION, nodes = emptyList())

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
    fun `find matching benchmarks using instance type`() {
        // given
        val instanceService = InstanceService(instanceRepository, awsService, parser)
        val instance = TEMPLATE_INSTANCE
        val configuration = TEMPLATE_CONFIGURATION.copy(instanceType = listOf("t3.micro", "t2.micro"))
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(TEMPLATE_INSTANCE, result.instance)
    }

    @Test
    fun `find matching benchmarks using instance tags`() {
        // given
        val instanceService = InstanceService(instanceRepository, awsService, parser)
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceTags = listOf(
                listOf("8 vCPUs", "16 GiB Memory"),
                listOf("8 vCPUs", "8 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks while multiple instance tags match`() {
        // given
        val instanceService = InstanceService(instanceRepository, awsService, parser)
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceTags = listOf(
                listOf("8 vCPUs"),
                listOf("8 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks when no matches`() {
        // given
        val instanceService = InstanceService(instanceRepository, awsService, parser)
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceType = listOf("t3.small", "t2.small"),
            instanceTags = listOf(
                listOf("16 vCPUs"),
                listOf("16 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(0, result.benchmarks.size)
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks when instance type and instance tags match`() {
        // given
        val instanceService = InstanceService(instanceRepository, awsService, parser)
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration = TEMPLATE_CONFIGURATION.copy(
            instanceType = listOf("t3.small", "t2.micro"),
            instanceTags = listOf(
                listOf("16 vCPUs"),
                listOf("8 GiB Memory")
            )
        )
        val benchmark = TEMPLATE_BENCHMARK.copy(configuration = configuration)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark))

        // then
        assertEquals(1, result.benchmarks.size)
        assertEquals(benchmark, result.benchmarks[0])
        assertEquals(instance, result.instance)
    }

    @Test
    fun `find matching benchmarks with only nulls`() {
        // given
        val instanceService = InstanceService(instanceRepository, awsService, parser)
        val instance = TEMPLATE_INSTANCE
        val benchmarks = listOf(TEMPLATE_BENCHMARK)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, benchmarks)

        // then
        assertEquals(0, result.benchmarks.size)
        assertEquals(TEMPLATE_INSTANCE, result.instance)
    }

    @Test
    fun `find matching benchmarks when multiple benchmarks match`() {
        // given
        val instanceService = InstanceService(instanceRepository, awsService, parser)
        val instance = TEMPLATE_INSTANCE.copy(tags = listOf("8 vCPUs", "8 GiB Memory"))
        val configuration1 = TEMPLATE_CONFIGURATION.copy(instanceType = listOf("t2.micro"))
        val benchmark1 = TEMPLATE_BENCHMARK.copy(configuration = configuration1)
        val configuration2 = TEMPLATE_CONFIGURATION.copy(instanceTags = listOf(listOf("8 vCPUs")))
        val benchmark2 = TEMPLATE_BENCHMARK.copy(configuration = configuration2)

        // when
        val result = instanceService.findMatchingBenchmarks(instance, listOf(benchmark1, benchmark2))

        // then
        assertEquals(2, result.benchmarks.size)
        assertEquals(benchmark1, result.benchmarks[0])
        assertEquals(benchmark2, result.benchmarks[1])
        assertEquals(instance, result.instance)
    }

    @Test
    fun `save instance if not in database`() = runTest {
        // given
        val service = InstanceService(instanceRepository, awsService, parser)
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
        val service = InstanceService(instanceRepository, awsService, parser)
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
        val service = InstanceService(instanceRepository, awsService, parser)
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