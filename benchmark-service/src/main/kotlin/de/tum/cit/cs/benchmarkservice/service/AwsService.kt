package de.tum.cit.cs.benchmarkservice.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.ec2.Ec2AsyncClient
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo


@Service
class AwsService(
    private val ec2Client: Ec2AsyncClient
) {
    private val logger = KotlinLogging.logger {}

    suspend fun getInstancesFromAws(): List<InstanceTypeInfo> {
        logger.info { "Starting downloading data about instances from AWS" }
        var nextToken: String? = ""
        var request = DescribeInstanceTypesRequest.builder().build()
        val instanceTypeInfos = mutableListOf<InstanceTypeInfo>()
        while (nextToken != null) {
            val response = ec2Client.describeInstanceTypes(request).await()
            val newInstances = response.instanceTypes()
            logger.debug { "Downloaded information about ${newInstances.size} instances" }
            instanceTypeInfos.addAll(newInstances)
            nextToken = response.nextToken()
            request = DescribeInstanceTypesRequest.builder().nextToken(nextToken).build()
        }
        logger.info {
            "Finished downloading data about instances from AWS. " +
                    "Downloaded information about ${instanceTypeInfos.size} instances."
        }
        return instanceTypeInfos
    }
}
