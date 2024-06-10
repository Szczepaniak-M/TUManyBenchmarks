package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.*
import de.tum.cit.cs.benchmarkservice.model.Instance
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*


@Service
class AwsService(
    private val ec2Client: Ec2Client
) {
    @Value("\${aws.ec2.default-ami.x86_64}")
    lateinit var amiX86_64: String

    @Value("\${aws.ec2.default-ami.arm}")
    lateinit var amiArm: String

    @Value("\${aws.ec2.private-key.name}")
    lateinit var keyPairName: String

    @Value("\${aws.ec2.network.availability-zone}")
    lateinit var networkAvailabilityZone: String

    @Value("\${aws.ec2.network.cidr}")
    lateinit var networkCidr: String

    private val logger = KotlinLogging.logger {}

    suspend fun getInstancesFromAws(): List<InstanceTypeInfo> {
        logger.info { "Starting downloading data about instances from AWS" }
        var nextTokenFromResponse: String? = ""
        var request = DescribeInstanceTypesRequest {}
        val instanceTypeInfos = mutableListOf<InstanceTypeInfo>()
        while (nextTokenFromResponse != null) {
            val response = retryIfException {
                ec2Client.describeInstanceTypes(request)
            }
            val newInstances = response.instanceTypes
            logger.debug { "Downloaded information about ${newInstances?.size} instances" }
            instanceTypeInfos.addAll(newInstances ?: emptyList())
            nextTokenFromResponse = response.nextToken
            request = DescribeInstanceTypesRequest { nextToken = nextTokenFromResponse }
        }
        logger.info {
            "Finished downloading data about instances from AWS. " +
                    "Downloaded information about ${instanceTypeInfos.size} instances."
        }
        return instanceTypeInfos
    }

    suspend fun createVpc(): String {
        logger.info { "Starting creating  VPC" }
        val createVpcRequest = CreateVpcRequest { cidrBlock = networkCidr }
        val createVpcResponse = retryIfException { ec2Client.createVpc(createVpcRequest) }
        val newVpcId = createVpcResponse.vpc?.vpcId ?: throw RuntimeException("VPC creation failed")
        val modify = ModifyVpcAttributeRequest {
            vpcId = newVpcId
            enableDnsSupport = AttributeBooleanValue { value = true }
            enableDnsHostnames = AttributeBooleanValue { value = true }
        }
        retryIfException { ec2Client.modifyVpcAttribute(modify) }
        logger.info { "Created VPC $newVpcId" }
        return newVpcId
    }

    suspend fun createSubnet(vpc: String): String {
        logger.info { "Starting creating subnet for VPC $vpc" }
        val createSubnetRequest = CreateSubnetRequest {
            vpcId = vpc
            cidrBlock = networkCidr
            availabilityZoneId = networkAvailabilityZone
        }
        val createSubnetResponse = retryIfException { ec2Client.createSubnet(createSubnetRequest) }
        val subnetId = createSubnetResponse.subnet?.subnetId ?: throw RuntimeException("Subnet creation failed")
        logger.info { "Created subnetId $subnetId for VPC $vpc" }
        return subnetId
    }

    suspend fun createSecurityGroup(vpc: String): String {
        logger.info { "Starting creating security group for VPC $vpc" }
        val randomUuid = UUID.randomUUID()
        val createRequest = CreateSecurityGroupRequest {
            groupName = "benchmark-$randomUuid"
            description = "benchmark-$randomUuid"
            vpcId = vpc
        }
        val createSecurityGroupResponse = retryIfException { ec2Client.createSecurityGroup(createRequest) }
        val newGroupId = createSecurityGroupResponse.groupId ?: throw RuntimeException("Security group creation failed")

        val ipPermissionIngress = IpPermission {
            fromPort = 0
            toPort = 65535
            ipProtocol = "-1"
            ipRanges = listOf(IpRange { cidrIp = networkCidr })
        }
        val ingressRequest = AuthorizeSecurityGroupIngressRequest {
            groupId = newGroupId
            ipPermissions = listOf(ipPermissionIngress)
        }
        retryIfException { ec2Client.authorizeSecurityGroupIngress(ingressRequest) }

        val ipPermissionEgress = IpPermission {
            fromPort = 0
            toPort = 65535
            ipProtocol = "-1"
            ipRanges = listOf(IpRange { cidrIp = "0.0.0.0/0" })
        }
        val egressRequest = AuthorizeSecurityGroupEgressRequest {
            groupId = newGroupId
            ipPermissions = listOf(ipPermissionEgress)
        }
        retryIfException { ec2Client.authorizeSecurityGroupEgress(egressRequest) }

        logger.info { "Created security group $newGroupId for VPC $vpc" }
        return newGroupId
    }

    suspend fun startEc2Instance(instance: Instance, count: Int, subnet: String, securityGroup: String): List<String> {
        logger.info { "Starting EC2 $count instances" }
        val image = if (instance.tags.contains("arm64")) amiArm else amiX86_64
        val specification = RequestSpotLaunchSpecification {
            imageId = image
            instanceType = InstanceType.fromValue(instance.name)
            keyName = keyPairName
            securityGroups = listOf(securityGroup)
            subnetId = subnet
        }
        val spotRequest = RequestSpotInstancesRequest {
            instanceCount = count
            type = SpotInstanceType.OneTime
            launchSpecification = specification
        }
        val response = retryIfException { ec2Client.requestSpotInstances(spotRequest) }
        val instanceRequests = response.spotInstanceRequests ?: emptyList()
        val instanceIds = instanceRequests.mapNotNull { it.instanceId }
        return instanceIds
    }

    suspend fun getEc2InstanceAddresses(instances: List<String>): List<String> {
        val instanceCount = instances.size
        val describeInstancesRequest = DescribeInstancesRequest {
            instanceIds = instances
        }

        var publicDnsNames = emptyList<String>()
        var isAllRunning = false

        while (!isAllRunning) {
            val describeInstancesResponse = retryIfException { ec2Client.describeInstances(describeInstancesRequest) }
            val reservations = describeInstancesResponse.reservations ?: emptyList()
            val runningInstances = reservations.flatMap { it.instances ?: emptyList() }
                .filter { instances.contains(it.instanceId) && it.state?.name == InstanceStateName.Running }
            if (runningInstances.size != instanceCount) {
                delay(3000)
            } else {
                isAllRunning = true
                publicDnsNames = runningInstances.mapNotNull { it.publicDnsName }
            }
        }
        logger.info { "EC2 instances $instances are running" }
        return publicDnsNames
    }

    suspend fun terminateEc2Instance(instances: List<String>) {
        val terminateInstancesRequest = TerminateInstancesRequest {
            instanceIds = instances
        }
        retryIfException { ec2Client.terminateInstances(terminateInstancesRequest) }
        logger.info { "Stopped instances $instances" }
    }

    suspend fun deleteSecurityGroup(securityGroup: String) {
        val deleteRequest = DeleteSecurityGroupRequest {
            groupId = securityGroup
        }
        retryIfException { ec2Client.deleteSecurityGroup(deleteRequest) }
        logger.info { "Deleted security group $securityGroup" }
    }

    suspend fun deleteSubnet(subnet: String) {
        val deleteRequest = DeleteSubnetRequest {
            subnetId = subnet
        }
        retryIfException { ec2Client.deleteSubnet(deleteRequest) }
        logger.info { "Deleted subnet $subnet" }
    }

    suspend fun deleteVpc(vpc: String) {
        val deleteRequest = DeleteVpcRequest {
            vpcId = vpc
        }
        retryIfException { ec2Client.deleteVpc(deleteRequest) }
        logger.info { "Deleted security group $vpc" }
    }

    private suspend fun <T> retryIfException(action: suspend () -> T): T {
        var attempts = 0

        while (attempts < 3) {
            try {
                return action()
            } catch (e: Ec2Exception) {
                attempts++
                logger.error { "Encountered Ec2Exception: ${e.message}. Attempt $attempts of 3." }

                if (attempts >= 3) {
                    logger.error { "Encountered 3 Ec2Exceptions. Stopping execution." }
                    throw e
                }
            } catch (e: Exception) {
                logger.error { "An unexpected error occurred: ${e.message}. Stopping execution." }
                throw e
            }
        }
        throw IllegalStateException("This code should not be executed.")
    }

}
