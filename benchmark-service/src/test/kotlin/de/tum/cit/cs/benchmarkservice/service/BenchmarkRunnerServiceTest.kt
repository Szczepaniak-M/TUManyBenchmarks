package de.tum.cit.cs.benchmarkservice.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
class BenchmarkRunnerServiceTest {

    private lateinit var benchmarkRunnerService: BenchmarkRunnerService

    @MockkBean
    private lateinit var ec2ConfigurationService: Ec2ConfigurationService

    @MockkBean
    private lateinit var awsService: AwsService

    @MockkBean
    private lateinit var sshService: SshService

    @MockkBean
    private lateinit var outputParserService: OutputParserService

    companion object {
        private val EC2_CONFIGURATION = mockk<Ec2Configuration>()
        private const val INSTANCE_ID = "ID"
        private const val INSTANCE_NAME = "t2.micro"
        private const val BENCHMARK_ID = "BenchmarkId"
        private val INSTANCE = Instance(INSTANCE_ID, INSTANCE_NAME, 8, BigDecimal(4), "Up to 25 Gigabit", emptyList())
        private val CONFIGURATION = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), listOf("t2.micro")
        )
        private val NODES = listOf(
            Node(1, "t2.micro", "image", "ansible.yml", "./run", "cat text.txt"),
            Node(2, "t2.micro", "image", "ansible.yml", "./run", "cat text.txt")
        )
        private val BENCHMARK = Benchmark(BENCHMARK_ID, CONFIGURATION, NODES)
        private val INSTANCE_WITH_BENCHMARKS = InstanceWithBenchmarks(INSTANCE, listOf(BENCHMARK))
        private val BENCHMARK_OUTPUT = listOf("""{"key1": 1}", "{"key2": 2}""")
        private val BENCHMARK_RESULT = BenchmarkResult(INSTANCE_ID, INSTANCE_NAME, BENCHMARK_ID, mapOf("key1" to 1, "key2" to 2), 1)
        private val TERMINATED_INSTANCES = listOf("ec2-1", "ec2-2")
    }

    @BeforeEach
    fun setUp() {
        benchmarkRunnerService = BenchmarkRunnerService(
            ec2ConfigurationService,
            awsService,
            sshService,
            outputParserService
        )
    }

    @Test
    fun `should run benchmarks and return results`() = runTest {
        // given
        coEvery {
            ec2ConfigurationService.generateEc2Configuration(
                INSTANCE,
                BENCHMARK,
                any<String>()
            )
        } returns EC2_CONFIGURATION
        coEvery { awsService.createVpc(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.createInternetGateway(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.configureRouteTable(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.createSubnet(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.createSecurityGroup(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.startEc2Instance(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.getEc2InstanceAddresses(EC2_CONFIGURATION) } just Runs
        coEvery { sshService.executeBenchmark(EC2_CONFIGURATION) } returns BENCHMARK_OUTPUT
        coEvery { outputParserService.parseOutput(INSTANCE, BENCHMARK, BENCHMARK_OUTPUT) } returns BENCHMARK_RESULT
        coEvery { awsService.terminateEc2Instance(EC2_CONFIGURATION) } returns TERMINATED_INSTANCES
        coEvery { awsService.deleteSecurityGroup(EC2_CONFIGURATION, TERMINATED_INSTANCES) } just Runs
        coEvery { awsService.deleteSubnet(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.deleteInternetGateway(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.deleteVpc(EC2_CONFIGURATION) } just Runs

        // when
        val results = benchmarkRunnerService.runBenchmarksForInstance(INSTANCE_WITH_BENCHMARKS)

        // then
        assert(results.isNotEmpty())
        assert(results[0] == BENCHMARK_RESULT)

        coVerify(exactly = 1) { ec2ConfigurationService.generateEc2Configuration(INSTANCE, BENCHMARK, any<String>()) }
        coVerify(exactly = 1) { awsService.createVpc(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.createInternetGateway(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.configureRouteTable(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.createSubnet(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.createSecurityGroup(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.startEc2Instance(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.getEc2InstanceAddresses(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { sshService.executeBenchmark(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { outputParserService.parseOutput(INSTANCE, BENCHMARK, BENCHMARK_OUTPUT) }
        coVerify(exactly = 1) { awsService.terminateEc2Instance(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.deleteSecurityGroup(EC2_CONFIGURATION, TERMINATED_INSTANCES) }
        coVerify(exactly = 1) { awsService.deleteSubnet(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.deleteInternetGateway(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.deleteVpc(EC2_CONFIGURATION) }
    }

    @Test
    fun `should handle exception during benchmark execution`() = runTest {
        // given
        val terminatedInstances = listOf("ec2-1", "ec2-2")

        coEvery {
            ec2ConfigurationService.generateEc2Configuration(
                INSTANCE,
                BENCHMARK,
                any<String>()
            )
        } returns EC2_CONFIGURATION
        coEvery { awsService.createVpc(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.createInternetGateway(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.configureRouteTable(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.createSubnet(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.createSecurityGroup(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.startEc2Instance(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.getEc2InstanceAddresses(EC2_CONFIGURATION) } just Runs
        coEvery { sshService.executeBenchmark(EC2_CONFIGURATION) } throws RuntimeException("Execution failed")
        coEvery { awsService.terminateEc2Instance(EC2_CONFIGURATION) } returns terminatedInstances
        coEvery { awsService.deleteSecurityGroup(EC2_CONFIGURATION, terminatedInstances) } just Runs
        coEvery { awsService.deleteSubnet(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.deleteInternetGateway(EC2_CONFIGURATION) } just Runs
        coEvery { awsService.deleteVpc(EC2_CONFIGURATION) } just Runs

        // when
        val results = benchmarkRunnerService.runBenchmarksForInstance(INSTANCE_WITH_BENCHMARKS)

        // then
        assert(results.isEmpty())

        coVerify(exactly = 1) { ec2ConfigurationService.generateEc2Configuration(INSTANCE, BENCHMARK, any<String>()) }
        coVerify(exactly = 1) { awsService.createVpc(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.createInternetGateway(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.configureRouteTable(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.createSubnet(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.createSecurityGroup(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.startEc2Instance(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.getEc2InstanceAddresses(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { sshService.executeBenchmark(EC2_CONFIGURATION) }
        coVerify(exactly = 0) { outputParserService.parseOutput(any(), any(), any()) }
        coVerify(exactly = 1) { awsService.terminateEc2Instance(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.deleteSecurityGroup(EC2_CONFIGURATION, terminatedInstances) }
        coVerify(exactly = 1) { awsService.deleteSubnet(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.deleteInternetGateway(EC2_CONFIGURATION) }
        coVerify(exactly = 1) { awsService.deleteVpc(EC2_CONFIGURATION) }
    }
}
