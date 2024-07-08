package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import de.tum.cit.cs.benchmarkservice.model.NodeConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
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

    suspend fun executeBenchmark(ec2Configuration: Ec2Configuration, curls: List<String>): List<Pair<NodeConfig, String>> {
        val directory = ec2Configuration.directory
        var output = emptyList<Pair<NodeConfig, String>>()
        val privateKeyPath = Paths.get(ClassPathResource(filePath).uri)
        val loader = SecurityUtils.getKeyPairResourceParser()
        val keyPairs = withContext(Dispatchers.IO) {
            loader.loadKeyPairs(null, privateKeyPath, null)
        }
        val sshClient = SshClient.setUpDefaultClient()
        sshClient.start()
        try {
            sshClient.use { client ->
                // 1. Authenticate and setup sessions
                logger.info { "Starting sessions" }
                val nodeWithSession = ec2Configuration.nodes.map { node ->
                    val session = retryIfException {
                        client.connect("ubuntu", node.ipv6, 22).verify().session.apply {
                            addPublicKeyIdentity(keyPairs.first())
                            auth().verify()
                        }
                    }
                    node to session
                }
                // 2. Phase 1: Setup
                logger.info { "Set up nodes" }
                coroutineScope {
                    nodeWithSession.map { (node, session) ->
                        async(Dispatchers.IO) {
                            setUpNode(ec2Configuration, node, session, curls)
                        }
                    }.awaitAll()
                }

                // 3. Phase 2: Run benchmark
                logger.info { "Execute benchmark" }
                coroutineScope {
                    nodeWithSession.map { (node, session) ->
                        async(Dispatchers.IO) {
                            executeBenchmark(node, session, directory)
                        }
                    }.awaitAll()
                }

                // 4. Phase 3: Fetch results
                logger.info { "Print results" }
                coroutineScope {
                    output = nodeWithSession.map { (node, session) ->
                        async(Dispatchers.IO) {
                            getBenchmarkOutput(node, session, directory)
                        }
                    }.awaitAll()
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

    private suspend fun setUpNode(
        ec2Configuration: Ec2Configuration,
        node: NodeConfig, session: ClientSession,
        curls: List<String>
    ): String {
        val ansibleFile = node.ansibleConfiguration
        val commands = mutableListOf<String>()
        commands.addAll(curls)
        commands.add("cd ${ec2Configuration.directory}")
        if (ansibleFile?.isNotEmpty() == true) {
            commands.add("ansible-playbook --connection=local --inventory 127.0.0.1, $ansibleFile")
        }
        commands.addAll(createEtcHosts(ec2Configuration))
        val nodeIdCommand = "echo 'export NODE_ID=\"${node.nodeId}\"' >> ~/.bashrc"
        val updateSource = "source ~/.bashrc"
        commands.add(nodeIdCommand)
        commands.add(updateSource)

        val combinedCommands = commands.joinToString(" ; ")

        val outputStream = ByteArrayOutputStream()
        val execChannel = session.createExecChannel(combinedCommands)
        execChannel.out = outputStream

        execChannel.open().verify()
        execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0)

        return outputStream.toString()
    }

    private fun createEtcHosts(ec2Configuration: Ec2Configuration): List<String> {
        val etcHostsCommand = mutableListOf<String>()
        for (node in ec2Configuration.nodes) {
            if (node.nodeId > 0) {
                etcHostsCommand.add("echo \"${node.ipv4} node-${node.nodeId}\" | sudo tee -a /etc/hosts")
                etcHostsCommand.add("echo \"${node.ipv6} node-${node.nodeId}\" | sudo tee -a /etc/hosts")
            }
        }
        return etcHostsCommand
    }


    private suspend fun executeBenchmark(node: NodeConfig, session: ClientSession, directory: String): String {
        val outputStream = ByteArrayOutputStream()
        val execChannel = session.createExecChannel("cd ${directory}; ${node.benchmarkCommand}")
        execChannel.out = outputStream

        execChannel.open().verify()
        execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0)
        logger.info { "executeBenchmark output: $outputStream" }

        return outputStream.toString()
    }

    private suspend fun getBenchmarkOutput(node: NodeConfig, session: ClientSession, directory: String): Pair<NodeConfig, String> {
        val outputStream = ByteArrayOutputStream()
        val execChannel = session.createExecChannel("cd ${directory}; ${node.outputCommand}")
        execChannel.out = outputStream

        execChannel.open().verify()
        execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0)
        logger.info { "getBenchmarkOutput output: $outputStream" }
        return node to outputStream.toString()
    }

    private suspend fun <T> retryIfException(action: suspend () -> T): T {
        var attempts = 0

        while (attempts < 3) {
            try {
                return action()
            } catch (e: Exception) {
                attempts++
                logger.warn { "Encountered ${e.cause}: ${e.message} Attempt $attempts of 3." }

                if (attempts >= 3) {
                    logger.error { "Encountered 3 Exceptions. Stopping execution." }
                    throw e
                }
                delay(15_000)
            }
        }
        throw IllegalStateException("This code should not be executed.")
    }
}
