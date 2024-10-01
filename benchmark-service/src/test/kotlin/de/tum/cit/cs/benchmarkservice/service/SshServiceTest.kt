package de.tum.cit.cs.benchmarkservice.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import de.tum.cit.cs.benchmarkservice.model.NodeConfig
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ChannelExec
import org.apache.sshd.client.session.ClientSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.ByteArrayOutputStream

@ExtendWith(SpringExtension::class)
class SshServiceTest {

    private lateinit var sshService: SshService

    @MockkBean
    private lateinit var gitHubService: GitHubService

    @MockkBean
    private lateinit var sshClient: SshClient

    @MockkBean
    private lateinit var mockSession: ClientSession

    @MockkBean
    private lateinit var execChannel: ChannelExec

    @BeforeEach
    fun setUp() {
        sshService = SshService(gitHubService, "aws-benchmark-private-key.pem")
        mockkStatic(SshClient::setUpDefaultClient)
        every { SshClient.setUpDefaultClient() } returns sshClient
    }

    @Test
    fun `test executeBenchmark should return expected output`() = runBlocking {
        // given
        val nodeConfig = NodeConfig(
            1, null, "t2.micro", "image", "ansible.yml",
            "./run", "cat test.json", "127.0.0.1", "10::1"
        )
        val ec2Config = Ec2Configuration("benchmark-run-1", "test-dir", listOf(nodeConfig))

        every { sshClient.start() } just Runs
        every { sshClient.stop() } just Runs
        every { sshClient.close() } just Runs
        every { sshClient.connect("ubuntu", "10::1", 22) } returns mockk {
            every { verify() } returns mockk {
                every { session } returns mockSession
            }
        }
        every { sshClient.properties } returns  mutableMapOf<String, Object>()

        every { mockSession.auth() } returns mockk {
            every { verify() } returns mockk()
        }
        every { mockSession.addPublicKeyIdentity(any()) } just Runs
        every { mockSession.createExecChannel(any<String>()) } returns execChannel

        every { execChannel.open() } returns mockk {
            every { verify() } returns mockk()
        }
        every { execChannel.setOut(any<ByteArrayOutputStream>()) } answers {
            firstArg<ByteArrayOutputStream>().write("benchmark output".toByteArray())
            firstArg<ByteArrayOutputStream>()
        }
        every { execChannel.waitFor(any(), any<Long>()) } returns mockk()
        every { execChannel.close() } just Runs

        coEvery { gitHubService.getCurlsForFilesFromDirectory("test-dir") } returns listOf("curl link1", "curl link2")

        // Act
        val result = sshService.executeBenchmark(ec2Config)

        // Assert
        assertEquals(listOf("benchmark output"), result)

        verify(exactly = 1) { sshClient.start() }
        verify(exactly = 1) { sshClient.stop() }
        verify(exactly = 1) { sshClient.close() }
        verify(exactly = 1) { sshClient.connect("ubuntu", "10::1", 22) }
        verify(exactly = 1) { sshClient.properties }
        verify(exactly = 1) { mockSession.auth() }
        verify(exactly = 1) { mockSession.addPublicKeyIdentity(any()) }
        verify(exactly = 1) {
            mockSession.createExecChannel(
                """sudo apt-get update ; curl link1 ; curl link2 ; cd test-dir ; """
                        + """ANSIBLE_GATHERING=explicit ansible-playbook --connection=local --inventory 127.0.0.1, ansible.yml ; """
                        + """echo "127.0.0.1 node-1" | sudo tee -a /etc/hosts ; """
                        + """echo "10::1 node-1" | sudo tee -a /etc/hosts ; """
                        + """echo 'export NODE_ID="1"' >> ~/.bashrc ; source ~/.bashrc"""
            )
        }
        verify(exactly = 1) { mockSession.createExecChannel("cd test-dir; ./run") }
        verify(exactly = 1) { mockSession.createExecChannel("cd test-dir; cat test.json") }
        verify(exactly = 3) { execChannel.open() }
        verify(exactly = 2) { execChannel.setOut(any<ByteArrayOutputStream>()) }
        verify(exactly = 3) { execChannel.waitFor(any(), any<Long>()) }
        verify(exactly = 3) { execChannel.close() }
        coVerify(exactly = 1) { gitHubService.getCurlsForFilesFromDirectory("test-dir") }
    }

    @Test
    fun `test setUpNode should return false on failed setup and return empty list`() = runBlocking {
        // given
        val nodeConfig = NodeConfig(
            1, null, "t2.micro", "image", "ansible.yml",
            "./run", "cat test.json", "127.0.0.1", "10::1"
        )
        val ec2Config = Ec2Configuration("benchmark-run-1", "test-dir", listOf(nodeConfig))

        every { sshClient.start() } just Runs
        every { sshClient.stop() } just Runs
        every { sshClient.close() } just Runs
        every { sshClient.connect("ubuntu", "10::1", 22) } returns mockk {
            every { verify() } returns mockk {
                every { session } returns mockSession
            }
        }
        every { sshClient.properties } returns  mutableMapOf<String, Object>()

        every { mockSession.auth() } returns mockk {
            every { verify() } returns mockk()
        }
        every { mockSession.addPublicKeyIdentity(any()) } just Runs
        every { mockSession.createExecChannel(any<String>()) } returns execChannel

        every { execChannel.open() } returns mockk {
            every { verify() } returns mockk()
        }
        every { execChannel.setOut(any()) } answers {
            firstArg<ByteArrayOutputStream>().write("FAILED! Something went wrong".toByteArray())
            firstArg<ByteArrayOutputStream>()
        }
        every { execChannel.waitFor(any(), any<Long>()) } returns mockk()
        every { execChannel.close() } just Runs

        coEvery { gitHubService.getCurlsForFilesFromDirectory("test-dir") } returns listOf("link1", "link2")

        // when
        val result = sshService.executeBenchmark(ec2Config)

        // then
        assertTrue(result.isEmpty())
        verify(exactly = 1) { sshClient.start() }
        verify(exactly = 1) { sshClient.stop() }
        verify(exactly = 1) { sshClient.close() }
        verify(exactly = 1) { sshClient.connect("ubuntu", "10::1", 22) }
        verify(exactly = 1) { sshClient.properties }
        verify(exactly = 1) { mockSession.auth() }
        verify(exactly = 1) { mockSession.addPublicKeyIdentity(any()) }
        verify(exactly = 1) { mockSession.createExecChannel(any<String>()) }
        verify(exactly = 1) { execChannel.open() }
        verify(exactly = 1) { execChannel.setOut(any<ByteArrayOutputStream>()) }
        verify(exactly = 1) { execChannel.waitFor(any(), any<Long>()) }
        verify(exactly = 1) { execChannel.close() }
        coVerify(exactly = 1) { gitHubService.getCurlsForFilesFromDirectory("test-dir") }
    }

    @Test
    fun `test executeBenchmark should return expected output when one node does not produce output`() = runBlocking {
        // given
        val nodeConfigClient = NodeConfig(
            1, null, "t2.micro", "image", "ansible.yml",
            "./run", "cat test.json", "127.0.0.1", "10::1"
        )
        val nodeConfigServer = NodeConfig(
            2, null, "t2.micro", "image", "ansible.yml",
            "./run", null, "127.0.0.2", "10::2"
        )
        val ec2Config = Ec2Configuration("benchmark-run-1", "test-dir", listOf(nodeConfigClient, nodeConfigServer))

        every { sshClient.start() } just Runs
        every { sshClient.stop() } just Runs
        every { sshClient.close() } just Runs
        every { sshClient.connect("ubuntu", "10::1", 22) } returns mockk {
            every { verify() } returns mockk {
                every { session } returns mockSession
            }
        }

        every { sshClient.properties } returns mutableMapOf<String, Object>()

        every { mockSession.auth() } returns mockk {
            every { verify() } returns mockk()
        }
        every { mockSession.addPublicKeyIdentity(any()) } just Runs
        every { mockSession.createExecChannel(any<String>()) } returns execChannel

        every { execChannel.open() } returns mockk {
            every { verify() } returns mockk()
        }
        every { execChannel.setOut(any<ByteArrayOutputStream>()) } answers {
            firstArg<ByteArrayOutputStream>().write("benchmark output".toByteArray())
            firstArg<ByteArrayOutputStream>()
        }
        every { execChannel.waitFor(any(), any<Long>()) } returns mockk()
        every { execChannel.close() } just Runs

        val serverSessionMock = mockk<ClientSession>()
        val serverExecChannelMock = mockk<ChannelExec>()
        every { sshClient.connect("ubuntu", "10::2", 22) } returns mockk {
            every { verify() } returns mockk {
                every { session } returns serverSessionMock
            }
        }

        every { serverSessionMock.auth() } returns mockk {
            every { verify() } returns mockk()
        }
        every { serverSessionMock.addPublicKeyIdentity(any()) } just Runs
        every { serverSessionMock.createExecChannel(any<String>()) } returns serverExecChannelMock

        every { serverExecChannelMock.open() } returns mockk {
            every { verify() } returns mockk()
        }
        every { serverExecChannelMock.setOut(any<ByteArrayOutputStream>()) } answers {
            firstArg<ByteArrayOutputStream>()
        }
        every { serverExecChannelMock.waitFor(any(), any<Long>()) } returns mockk()
        every { serverExecChannelMock.close() } just Runs

        coEvery { gitHubService.getCurlsForFilesFromDirectory("test-dir") } returns listOf("curl link1", "curl link2")

        // when
        val result = sshService.executeBenchmark(ec2Config)

        // then
        assertEquals(listOf("benchmark output"), result)

        verify(exactly = 1) { sshClient.start() }
        verify(exactly = 1) { sshClient.stop() }
        verify(exactly = 1) { sshClient.close() }
        verify(exactly = 1) { sshClient.connect("ubuntu", "10::1", 22) }
        verify(exactly = 1) { sshClient.connect("ubuntu", "10::2", 22) }
        verify(exactly = 1) { sshClient.properties }
        verify(exactly = 1) { mockSession.auth() }
        verify(exactly = 1) { mockSession.addPublicKeyIdentity(any()) }
        verify(exactly = 3) { mockSession.createExecChannel(any<String>()) }
        verify(exactly = 1) {
            mockSession.createExecChannel(
                """sudo apt-get update ; curl link1 ; curl link2 ; cd test-dir ; """
                        + """ANSIBLE_GATHERING=explicit ansible-playbook --connection=local --inventory 127.0.0.1, ansible.yml ; """
                        + """echo "127.0.0.1 node-1" | sudo tee -a /etc/hosts ; """
                        + """echo "10::1 node-1" | sudo tee -a /etc/hosts ; """
                        + """echo "127.0.0.2 node-2" | sudo tee -a /etc/hosts ; """
                        + """echo "10::2 node-2" | sudo tee -a /etc/hosts ; """
                        + """echo 'export NODE_ID="1"' >> ~/.bashrc ; source ~/.bashrc"""
            )
        }
        verify(exactly = 1) { mockSession.createExecChannel("cd test-dir; ./run") }
        verify(exactly = 1) { mockSession.createExecChannel("cd test-dir; cat test.json") }
        verify(exactly = 3) { execChannel.open() }
        verify(exactly = 2) { execChannel.setOut(any<ByteArrayOutputStream>()) }
        verify(exactly = 3) { execChannel.waitFor(any(), any<Long>()) }
        verify(exactly = 3) { execChannel.close() }

        verify(exactly = 1) { serverSessionMock.auth() }
        verify(exactly = 1) { serverSessionMock.addPublicKeyIdentity(any()) }
        verify(exactly = 2) { serverSessionMock.createExecChannel(any<String>()) }
        verify(exactly = 1) {
            serverSessionMock.createExecChannel(
                """sudo apt-get update ; curl link1 ; curl link2 ; cd test-dir ; """
                        + """ANSIBLE_GATHERING=explicit ansible-playbook --connection=local --inventory 127.0.0.1, ansible.yml ; """
                        + """echo "127.0.0.1 node-1" | sudo tee -a /etc/hosts ; """
                        + """echo "10::1 node-1" | sudo tee -a /etc/hosts ; """
                        + """echo "127.0.0.2 node-2" | sudo tee -a /etc/hosts ; """
                        + """echo "10::2 node-2" | sudo tee -a /etc/hosts ; """
                        + """echo 'export NODE_ID="2"' >> ~/.bashrc ; source ~/.bashrc"""
            )
        }
        verify(exactly = 1) { serverSessionMock.createExecChannel("cd test-dir; ./run") }
        verify(exactly = 2) { serverExecChannelMock.open() }
        verify(exactly = 1) { serverExecChannelMock.setOut(any<ByteArrayOutputStream>()) }
        verify(exactly = 2) { serverExecChannelMock.waitFor(any(), any<Long>()) }
        verify(exactly = 2) { serverExecChannelMock.close() }
        coVerify(exactly = 2) { gitHubService.getCurlsForFilesFromDirectory("test-dir") }
    }
}
