package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.model.InstanceTypeInfo
import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.model.InstanceWithBenchmarks
import de.tum.cit.cs.benchmarkservice.repository.InstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class InstanceService(
    private val instanceRepository: InstanceRepository,
    private val awsService: AwsService,
    private val instanceTypeInfoParser: InstanceTypeInfoParser,
) {

    private val logger = KotlinLogging.logger {}

    fun updateInstances() = runBlocking {
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

    fun findMatchingBenchmarks(instance: Instance, benchmarks: List<Benchmark>): InstanceWithBenchmarks {
        val applicableBenchmarks = mutableListOf<Benchmark>()
        for (benchmark in benchmarks) {
            if (benchmark.configuration.instanceType?.contains(instance.name) == true) {
                applicableBenchmarks.add(benchmark)
            } else if (benchmark.configuration.instanceTags != null) {
                for (tagList in benchmark.configuration.instanceTags) {
                    if (instance.tags.containsAll(tagList)) {
                        applicableBenchmarks.add(benchmark)
                        break
                    }
                }
            }
        }
        return InstanceWithBenchmarks(instance, applicableBenchmarks)
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
