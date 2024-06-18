package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.util.security.SecurityUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.util.*


@Service
class SshService {

    @Value("\${aws.ec2.private-key.file}")
    lateinit var filePath: String

    private val logger = KotlinLogging.logger {}

    // TODO add executing benchmarks using multiple nodes
    //   - network IP of other nodes
    //   - detecting own IP by node
    //   - executing different ansible for different nodes
    fun executeBenchmark(benchmark: Benchmark, nodesDomains: List<String>, curls: List<String>): List<String> {
        val output = mutableListOf<String>()
        val privateKeyPath = Paths.get(ClassPathResource(filePath).uri)
        val loader = SecurityUtils.getKeyPairResourceParser()
        val keyPairs = loader.loadKeyPairs(null, privateKeyPath, null)
        val sshClient = SshClient.setUpDefaultClient()
        sshClient.start()
        try {
            sshClient.use { client ->
                for (nodesDomain in nodesDomains) {
                    val session: ClientSession? = client.connect("ubuntu", nodesDomain, 22)
                        .verify().session
                    session?.use {
                        it.addPublicKeyIdentity(keyPairs.iterator().next())
                        it.auth().verify()

                        val ansibleFile = benchmark.nodes[0].ansibleConfiguration
                        val commands = mutableListOf<String>()
                        commands.addAll(curls)
                        commands.add("ansible-playbook --connection=local --inventory 127.0.0.1, $ansibleFile")
                        commands.add(benchmark.nodes[0].benchmarkCommand!!)
                        commands.add(benchmark.nodes[0].outputCommand!!)
                        val combinedCommands = commands.joinToString(" ; ")

                        val outputStream = ByteArrayOutputStream()
                        val execChannel = it.createExecChannel(combinedCommands)
                        execChannel.out = outputStream

                        execChannel.open().verify()
                        execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0)

                        val result = outputStream.toString()

                        val outputLines = result.lines()
                        output.add(outputLines.lastOrNull { it.isNotBlank() } ?: "")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "Error occurred during SSH communication: ${e.message}" }
            throw e
        } finally {
            sshClient.stop()
        }
        return output
    }
}