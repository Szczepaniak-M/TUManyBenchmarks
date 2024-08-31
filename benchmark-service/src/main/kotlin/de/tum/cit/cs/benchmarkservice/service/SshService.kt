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
class SshService(
    private val gitHubService: GitHubService
) {

    @Value("\${aws.ec2.private-key.file}")
    lateinit var filePath: String

    private val logger = KotlinLogging.logger {}

    suspend fun executeBenchmark(ec2Configuration: Ec2Configuration): List<String> {
        val directory = ec2Configuration.directory
        var output = emptyList<String>()
        val privateKeyPath = Paths.get(ClassPathResource(filePath).uri)
        val loader = SecurityUtils.getKeyPairResourceParser()
        val keyPairs = withContext(Dispatchers.IO) {
            loader.loadKeyPairs(null, privateKeyPath, null)
        }
        val sshClient = SshClient.setUpDefaultClient()
        sshClient.start()
        try {
            sshClient.use { client ->
                logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Starting sessions" }
                coroutineScope {
                    val nodeWithSession = ec2Configuration.nodes.map { node ->
                        async(Dispatchers.IO) {
                            val session = retryIfException {
                                client.connect("ubuntu", node.ipv6, 22).verify().session.apply {
                                    addPublicKeyIdentity(keyPairs.first())
                                    auth().verify()
                                }
                            }
                            node to session
                        }
                    }.awaitAll()

                    // 2. Phase 1: Setup
                    logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Setting up nodes" }
                    val setupSucceeded = nodeWithSession.map { (node, session) ->
                        async(Dispatchers.IO) {
                            setUpNode(ec2Configuration, node, session)
                        }
                    }.awaitAll()
                        .fold(true) { prev, curr -> prev && curr }

                    if (!setupSucceeded) {
                        logger.error {
                            "Benchmark ${ec2Configuration.benchmarkRunId}: Set up for at least on of the nodes failed. "
                            "Stopping benchmark execution."
                        }
                    } else {
                        // 3. Phase 2: Run benchmark
                        logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Executing benchmark" }
                        nodeWithSession.map { (node, session) ->
                            async(Dispatchers.IO) {
                                executeBenchmark(node, session, directory)
                            }
                        }.awaitAll()

                        // 4. Phase 3: Fetch results
                        logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Extracting output" }
                        output = nodeWithSession.map { (node, session) ->
                            async(Dispatchers.IO) {
                                getBenchmarkOutput(node, session, directory)
                            }
                        }.awaitAll()
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

    private suspend fun setUpNode(
        ec2Configuration: Ec2Configuration,
        node: NodeConfig,
        session: ClientSession
    ): Boolean {
        val ansibleFile = node.ansibleConfiguration
        val combinedCommands = prepareSetUpCommands(ec2Configuration, ansibleFile, node)
        val execChannel = session.createExecChannel(combinedCommands)
        val outputStream = ByteArrayOutputStream()
        execChannel.out = outputStream
        execChannel.open().verify()
        execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0)
        val output = outputStream.toString()
        execChannel.close()
        if (output.contains("FAILED!")) {
            logger.error {
                "Benchmark ${ec2Configuration.benchmarkRunId}: Benchmark failed during setting up nodes. "
                "Error message: $output"
            }
            return false
        }
        return true
    }

    private suspend fun executeBenchmark(node: NodeConfig, session: ClientSession, directory: String) {
        val command = "cd ${directory}; ${node.benchmarkCommand}"
        val execChannel = session.createExecChannel(command)
        execChannel.open().verify()
        execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0)
        execChannel.close()
    }

    private suspend fun getBenchmarkOutput(
        node: NodeConfig,
        session: ClientSession,
        directory: String
    ): String {
        val execChannel = session.createExecChannel("cd ${directory}; ${node.outputCommand}")
        val outputStream = ByteArrayOutputStream()
        execChannel.out = outputStream
        execChannel.open().verify()
        execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0)
        val output = outputStream.toString()
        execChannel.close()
        return output
    }

    private suspend fun prepareSetUpCommands(
        ec2Configuration: Ec2Configuration,
        ansibleFile: String?,
        node: NodeConfig
    ): String {
        val commands = mutableListOf<String>()
        commands.add("sudo apt-get update")
        val curls = gitHubService.getCurlsForFilesFromDirectory(ec2Configuration.directory)
        commands.addAll(curls)
        commands.add("cd ${ec2Configuration.directory}")
        if (ansibleFile?.isNotEmpty() == true) {
            commands.add("ANSIBLE_GATHERING=explicit ansible-playbook --connection=local --inventory 127.0.0.1, $ansibleFile")
        }
        commands.addAll(createEtcHosts(ec2Configuration))
        val nodeIdCommand = """echo 'export NODE_ID="${node.nodeId}"' >> ~/.bashrc"""
        val updateSource = "source ~/.bashrc"
        commands.add(nodeIdCommand)
        commands.add(updateSource)
        return commands.joinToString(" ; ")
    }

    private fun createEtcHosts(ec2Configuration: Ec2Configuration): List<String> {
        val etcHostsCommand = mutableListOf<String>()
        for (node in ec2Configuration.nodes) {
            if (node.nodeId > 0) {
                etcHostsCommand.add("""echo "${node.ipv4} node-${node.nodeId}" | sudo tee -a /etc/hosts""")
                etcHostsCommand.add("""echo "${node.ipv6} node-${node.nodeId}" | sudo tee -a /etc/hosts""")
            }
        }
        return etcHostsCommand
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
