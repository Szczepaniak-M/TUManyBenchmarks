package de.tum.cit.cs.benchmarkservice.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.Configuration
import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.model.Node
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
class Ec2ConfigurationServiceTest {

    private lateinit var ec2ConfigurationService: Ec2ConfigurationService

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    @BeforeEach
    fun setUp() {
        ec2ConfigurationService = Ec2ConfigurationService(instanceRepository, "ami-x86", "ami-arm")
    }

    @Test
    fun `should generate EC2 configuration based on default configuration`() = runBlocking {
        // given
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", listOf("ARM64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), listOf("t2.micro")
        )
        val nodes = listOf(
            Node(0, null, "image86", "imageArm", "ansible.yml", "./run", "cat text.txt"),
            Node(1, null, null, null, null, null, null),
            Node(2, null, null, null, null, null, null)
        )
        val benchmark = Benchmark("benchId", configuration, nodes)
        val benchmarkRunId = "test-run-id"

        // when
        val ec2Configuration = ec2ConfigurationService.generateEc2Configuration(instance, benchmark, benchmarkRunId)

        // then
        assertEquals(2, ec2Configuration.nodes.size)

        assertEquals(1, ec2Configuration.nodes[0].nodeId)
        assertEquals("t2.micro", ec2Configuration.nodes[0].instanceType)
        assertEquals("imageArm", ec2Configuration.nodes[0].image)
        assertEquals("ansible.yml", ec2Configuration.nodes[0].ansibleConfiguration)
        assertEquals("./run", ec2Configuration.nodes[0].benchmarkCommand)
        assertEquals("cat text.txt", ec2Configuration.nodes[0].outputCommand)

        assertEquals(2, ec2Configuration.nodes[1].nodeId)
        assertEquals("t2.micro", ec2Configuration.nodes[1].instanceType)
        assertEquals("imageArm", ec2Configuration.nodes[1].image)
        assertEquals("ansible.yml", ec2Configuration.nodes[1].ansibleConfiguration)
        assertEquals("./run", ec2Configuration.nodes[1].benchmarkCommand)
        assertEquals("cat text.txt", ec2Configuration.nodes[1].outputCommand)

        coVerify(exactly = 0) { instanceRepository.findInstanceByName(any<String>()) }
    }

    @Test
    fun `should generate EC2 configuration should override default configuration`() = runBlocking {
        // given
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", listOf("ARM64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), listOf("t2.micro")
        )
        val nodes = listOf(
            Node(0, "t2.micro", "image86", "imageArm", "ansible.yml", "./run", "cat text.txt"),
            Node(1, null, null, null, null, null, null),
            Node(2, "t3.micro", "customImage86", "customImageArm", "custom.yml", "./runCustom", "python run.py")
        )
        val benchmark = Benchmark("benchId", configuration, nodes)
        val benchmarkRunId = "test-run-id"
        coEvery { instanceRepository.findInstanceByName("t3.micro") } returns instance.copy(
            name = "t3.micro",
            vCpu = 10,
            tags = listOf("X86-64")
        )

        // when
        val ec2Configuration = ec2ConfigurationService.generateEc2Configuration(instance, benchmark, benchmarkRunId)

        // then
        assertEquals(2, ec2Configuration.nodes.size)
        assertEquals(18, ec2Configuration.vcpuCost)

        assertEquals(1, ec2Configuration.nodes[0].nodeId)
        assertEquals("t2.micro", ec2Configuration.nodes[0].instanceType)
        assertEquals("imageArm", ec2Configuration.nodes[0].image)
        assertEquals("ansible.yml", ec2Configuration.nodes[0].ansibleConfiguration)
        assertEquals("./run", ec2Configuration.nodes[0].benchmarkCommand)
        assertEquals("cat text.txt", ec2Configuration.nodes[0].outputCommand)

        assertEquals(2, ec2Configuration.nodes[1].nodeId)
        assertEquals("t3.micro", ec2Configuration.nodes[1].instanceType)
        assertEquals("customImage86", ec2Configuration.nodes[1].image)
        assertEquals("custom.yml", ec2Configuration.nodes[1].ansibleConfiguration)
        assertEquals("./runCustom", ec2Configuration.nodes[1].benchmarkCommand)
        assertEquals("python run.py", ec2Configuration.nodes[1].outputCommand)

        coVerify(exactly = 1) { instanceRepository.findInstanceByName(any<String>()) }
    }

    @Test
    fun `should generate EC2 configuration based on default configuration when nodes missing`() = runBlocking {
        // given
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", listOf("ARM64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), listOf("t2.micro")
        )
        val nodes = listOf(
            Node(0, null, null, null, "ansible.yml", "./run", "cat text.txt"),
        )
        val benchmark = Benchmark("benchId", configuration, nodes)
        val benchmarkRunId = "test-run-id"

        // when
        val ec2Configuration = ec2ConfigurationService.generateEc2Configuration(instance, benchmark, benchmarkRunId)

        // then
        assertEquals(2, ec2Configuration.nodes.size)
        assertEquals(16, ec2Configuration.vcpuCost)

        assertEquals(1, ec2Configuration.nodes[0].nodeId)
        assertEquals("t2.micro", ec2Configuration.nodes[0].instanceType)
        assertEquals("ami-arm", ec2Configuration.nodes[0].image)
        assertEquals("ansible.yml", ec2Configuration.nodes[0].ansibleConfiguration)
        assertEquals("./run", ec2Configuration.nodes[0].benchmarkCommand)
        assertEquals("cat text.txt", ec2Configuration.nodes[0].outputCommand)

        assertEquals(2, ec2Configuration.nodes[1].nodeId)
        assertEquals("t2.micro", ec2Configuration.nodes[1].instanceType)
        assertEquals("ami-arm", ec2Configuration.nodes[1].image)
        assertEquals("ansible.yml", ec2Configuration.nodes[1].ansibleConfiguration)
        assertEquals("./run", ec2Configuration.nodes[1].benchmarkCommand)
        assertEquals("cat text.txt", ec2Configuration.nodes[1].outputCommand)

        coVerify(exactly = 0) { instanceRepository.findInstanceByName(any<String>()) }
    }

    @Test
    fun `should generate EC2 configuration with X86-64 instance image`() = runBlocking {
        // given
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", listOf("x86-64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), listOf("t2.micro")
        )
        val nodes = listOf(
            Node(0, "t2.micro", null, null, "ansible.yml", "./run", "cat text.txt"),
            Node(1, null, null, null, null, null, null),
            Node(2, null, null, null, null, null, null)
        )
        val benchmark = Benchmark("benchId", configuration, nodes)
        val benchmarkRunId = "test-run-id"

        // when
        val ec2Configuration = ec2ConfigurationService.generateEc2Configuration(instance, benchmark, benchmarkRunId)

        // then
        assertEquals(2, ec2Configuration.nodes.size)
        assertEquals(16, ec2Configuration.vcpuCost)
        assertEquals("ami-x86", ec2Configuration.nodes[0].image)
        assertEquals("ami-x86", ec2Configuration.nodes[1].image)

        coVerify(exactly = 0) { instanceRepository.findInstanceByName(any<String>()) }
    }

    @Test
    fun `should generate EC2 configuration with custom instance without custom image`() = runBlocking {
        // given
        val instance = Instance("id", "t2.micro", 8, BigDecimal(4), "Up to 25 Gigabit", listOf("x86-64"))
        val configuration = Configuration(
            "name", "description", "directory", "* * * * *",
            2, emptyList(), listOf("t2.micro")
        )
        val nodes = listOf(
            Node(0, "t2.micro", null, null, "ansible.yml", "./run", "cat text.txt"),
            Node(1, "t3.micro", null, null, null, null, null),
            Node(2, null, null, null, null, null, null)
        )
        val benchmark = Benchmark("benchId", configuration, nodes)
        val benchmarkRunId = "test-run-id"

        coEvery { instanceRepository.findInstanceByName("t3.micro") } returns instance.copy(
            name = "t3.micro",
            vCpu = 10,
            tags = listOf("ARM64")
        )

        // when
        val ec2Configuration = ec2ConfigurationService.generateEc2Configuration(instance, benchmark, benchmarkRunId)

        // then
        assertEquals(2, ec2Configuration.nodes.size)
        assertEquals(18, ec2Configuration.vcpuCost)
        assertEquals(1, ec2Configuration.nodes[0].nodeId)
        assertEquals("t3.micro", ec2Configuration.nodes[0].instanceType)
        assertEquals("ami-arm", ec2Configuration.nodes[0].image)
        assertEquals("ansible.yml", ec2Configuration.nodes[0].ansibleConfiguration)
        assertEquals("./run", ec2Configuration.nodes[0].benchmarkCommand)
        assertEquals("cat text.txt", ec2Configuration.nodes[0].outputCommand)

        assertEquals("ami-x86", ec2Configuration.nodes[1].image)

        coVerify(exactly = 1) { instanceRepository.findInstanceByName("t3.micro") }
    }
}
