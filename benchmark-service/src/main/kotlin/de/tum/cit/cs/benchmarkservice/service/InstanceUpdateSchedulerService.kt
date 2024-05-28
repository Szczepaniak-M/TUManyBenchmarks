package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo

@Service
class InstanceUpdateSchedulerService(
    private val instanceRepository: InstanceRepository,
    private val awsService: AwsService,
    private val instanceTypeInfoParser: InstanceTypeInfoParser,
) {

    private val logger = KotlinLogging.logger {}

    // run at application start
    // TODO discuss whether to change to every week?
    @EventListener(ApplicationReadyEvent::class)
    fun updateInstances() {
        logger.info { "Starting updating AWS EC2 instances information" }
        runBlocking {
            val awsInstances = getInstancesFromAws()
            val dbInstances = getInstancesFromDatabase().toList()
            awsInstances.map { awsInstance ->
                launch {
                    val matchingDbInstance = dbInstances.find { it.name == awsInstance.name }
                    if (matchingDbInstance == null) {
                        instanceRepository.save(awsInstance)
                        logger.debug { "Saved a new EC2 instance: ${awsInstance.name}" }
                    } else {
                        if (matchingDbInstance.tags != awsInstance.tags) {
                            instanceRepository.updateTagsById(matchingDbInstance.id!!, awsInstance.tags)
                            logger.debug { "Updated a EC2 instance: ${awsInstance.name}" }
                        }
                    }
                }
            }
        }
        logger.info { "Finished updating AWS EC2 instances information" }
    }

    private suspend fun getInstancesFromAws(): List<Instance> {
        return awsService.getInstancesFromAws()
            .map { parseInstanceTypeInfo(it) }
    }

    private fun parseInstanceTypeInfo(instanceTypeInfo: InstanceTypeInfo): Instance {
        val name = instanceTypeInfoParser.parseInstanceName(instanceTypeInfo)
        val tags = instanceTypeInfoParser.parseInstanceTags(instanceTypeInfo)
        return Instance(null, name, tags)
    }

    private fun getInstancesFromDatabase(): Flow<Instance> {
        return instanceRepository.findAll()
    }

}
