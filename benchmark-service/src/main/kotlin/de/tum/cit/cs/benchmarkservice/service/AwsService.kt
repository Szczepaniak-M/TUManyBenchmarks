package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.*
import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import de.tum.cit.cs.benchmarkservice.model.NodeConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*


@Service
class AwsService(
    private val ec2Client: Ec2Client
) {

    @Value("\${aws.ec2.private-key.name}")
    lateinit var keyPairName: String

    @Value("\${aws.ec2.network.availability-zone}")
    lateinit var networkAvailabilityZone: String

    @Value("\${aws.ec2.network.cidr}")
    lateinit var ipv4Cidr: String

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

    suspend fun createVpc(ec2Configuration: Ec2Configuration) {
        logger.info { "Starting creating  VPC" }
        ec2Configuration.ipv4Cidr = ipv4Cidr
        val createVpcRequest = CreateVpcRequest { cidrBlock = ec2Configuration.ipv4Cidr }
        val createVpcResponse = retryIfException { ec2Client.createVpc(createVpcRequest) }
        ec2Configuration.vpcId = createVpcResponse.vpc?.vpcId ?: throw RuntimeException("VPC creation failed")
        val dnsSupportRequest = ModifyVpcAttributeRequest {
            vpcId = ec2Configuration.vpcId
            enableDnsSupport = AttributeBooleanValue { value = true }
        }
        retryIfException { ec2Client.modifyVpcAttribute(dnsSupportRequest) }
        val enableDnsHostnamesRequest = ModifyVpcAttributeRequest {
            vpcId = ec2Configuration.vpcId
            enableDnsHostnames = AttributeBooleanValue { value = true }
        }
        retryIfException { ec2Client.modifyVpcAttribute(enableDnsHostnamesRequest) }
        val ipv6CidrRequest = AssociateVpcCidrBlockRequest {
            vpcId = ec2Configuration.vpcId
            amazonProvidedIpv6CidrBlock = true
        }
        val ipv6CidrResponse = retryIfException { ec2Client.associateVpcCidrBlock(ipv6CidrRequest) }
        ec2Configuration.ipv6Cidr = ipv6CidrResponse.ipv6CidrBlockAssociation?.ipv6CidrBlock
        while (ec2Configuration.ipv6Cidr == null) {
            delay(3000)
            val describeVpcRequest = DescribeVpcsRequest {
                vpcIds = listOf(ec2Configuration.vpcId!!)
                filters = listOf(
                    Filter {
                        name = "vpc-id"
                        values = listOf(ec2Configuration.vpcId!!)
                    }
                )
            }
            val describeVpcResponse = ec2Client.describeVpcs(describeVpcRequest)
            ec2Configuration.ipv6Cidr = describeVpcResponse.vpcs
                ?.firstOrNull()
                ?.ipv6CidrBlockAssociationSet
                ?.firstOrNull()
                ?.ipv6CidrBlock
        }
        logger.info { "Created VPC ${ec2Configuration.vpcId}" }
    }

    suspend fun createInternetGateway(ec2Configuration: Ec2Configuration) {
        logger.info { "Starting creating Internet Gateway for VPC ${ec2Configuration.vpcId}" }
        val createIgwRequest = CreateInternetGatewayRequest {}
        val createIgwResponse = retryIfException { ec2Client.createInternetGateway(createIgwRequest) }
        ec2Configuration.internetGatewayId = createIgwResponse.internetGateway?.internetGatewayId
        val attachIgwRequest = AttachInternetGatewayRequest {
            vpcId = ec2Configuration.vpcId
            internetGatewayId = ec2Configuration.internetGatewayId
        }
        retryIfException { ec2Client.attachInternetGateway(attachIgwRequest) }
        logger.info { "Created Internet Gateway ${ec2Configuration.internetGatewayId} for VPC ${ec2Configuration.vpcId}" }
    }

    suspend fun configureRouteTable(ec2Configuration: Ec2Configuration) {
        val describeRouteTablesRequest = DescribeRouteTablesRequest {
            filters = listOf(Filter {
                name = "vpc-id"
                values = listOf(ec2Configuration.vpcId!!)
            })
        }
        val describeRouteTablesResponse = retryIfException { ec2Client.describeRouteTables(describeRouteTablesRequest) }
        ec2Configuration.routeTableId = describeRouteTablesResponse.routeTables?.firstOrNull()?.routeTableId
        if (ec2Configuration.routeTableId == null) {
            val createRouteTableRequest = CreateRouteTableRequest {
                vpcId = ec2Configuration.vpcId
            }
            val createRouteTableResponse = retryIfException { ec2Client.createRouteTable(createRouteTableRequest) }
            ec2Configuration.routeTableId = createRouteTableResponse.routeTable?.routeTableId
        }
        val createIpv6RouteRequest = CreateRouteRequest {
            routeTableId = ec2Configuration.routeTableId
            destinationIpv6CidrBlock = "::/0"
            gatewayId = ec2Configuration.internetGatewayId
        }
        retryIfException { ec2Client.createRoute(createIpv6RouteRequest) }
    }

    suspend fun createSubnet(ec2Configuration: Ec2Configuration) {
        logger.info { "Starting creating subnet for VPC ${ec2Configuration.vpcId}" }
        val createSubnetRequest = CreateSubnetRequest {
            vpcId = ec2Configuration.vpcId
            cidrBlock = ec2Configuration.ipv4Cidr
            ipv6CidrBlock = ec2Configuration.ipv6Cidr
            availabilityZoneId = networkAvailabilityZone
        }
        val createSubnetResponse = retryIfException { ec2Client.createSubnet(createSubnetRequest) }
        ec2Configuration.subnetId = createSubnetResponse.subnet?.subnetId
        val modifySubnetRequest = ModifySubnetAttributeRequest {
            subnetId = ec2Configuration.subnetId
            assignIpv6AddressOnCreation = AttributeBooleanValue { value = true }
        }
        retryIfException { ec2Client.modifySubnetAttribute(modifySubnetRequest) }
        logger.info { "Created subnetId ${ec2Configuration.subnetId} for VPC ${ec2Configuration.vpcId}" }
    }

    suspend fun createSecurityGroup(ec2Configuration: Ec2Configuration) {
        logger.info { "Starting creating security group for VPC ${ec2Configuration.vpcId}" }
        val randomUuid = UUID.randomUUID()
        val createRequest = CreateSecurityGroupRequest {
            groupName = "benchmark-$randomUuid"
            description = "benchmark-$randomUuid"
            vpcId = ec2Configuration.vpcId
        }
        val createSecurityGroupResponse = retryIfException { ec2Client.createSecurityGroup(createRequest) }
        ec2Configuration.securityGroupId =
            createSecurityGroupResponse.groupId ?: throw RuntimeException("Security group creation failed")
        val ipv4PermissionIngress = IpPermission {
            ipProtocol = "-1"
            ipRanges = listOf(IpRange { cidrIp = ec2Configuration.ipv4Cidr })
        }
        val intraIpv6PermissionIngress = IpPermission {
            ipProtocol = "-1"
            ipv6Ranges = listOf(Ipv6Range { cidrIpv6 = ec2Configuration.ipv6Cidr })
        }
        val sshIpv6PermissionIngress = IpPermission {
            ipProtocol = Protocol.Tcp.toString()
            ipv6Ranges = listOf(Ipv6Range { cidrIpv6 = "::/0" })
            fromPort = 22
            toPort = 22
        }
        val ingressRequest = AuthorizeSecurityGroupIngressRequest {
            groupId = ec2Configuration.securityGroupId
            ipPermissions = listOf(ipv4PermissionIngress, intraIpv6PermissionIngress, sshIpv6PermissionIngress)
        }
        retryIfException { ec2Client.authorizeSecurityGroupIngress(ingressRequest) }
        logger.info { "Created security group ${ec2Configuration.securityGroupId} for VPC ${ec2Configuration.vpcId}" }
    }

    suspend fun startEc2Instance(ec2Configuration: Ec2Configuration) {
        logger.info { "Starting EC2 ${ec2Configuration.nodes.size} instances" }
        ec2Configuration.nodes
            .groupBy { it.instanceType to it.image }
            .values
            .forEach { ec2NodeConfigs ->
                val node = ec2NodeConfigs[0]
                val specification = RequestSpotLaunchSpecification {
                    instanceType = InstanceType.fromValue(node.instanceType)
                    imageId = node.image
                    keyName = keyPairName
                    securityGroupIds = listOf(ec2Configuration.securityGroupId!!)
                    subnetId = ec2Configuration.subnetId

                }
                val spotRequestRequest = RequestSpotInstancesRequest {
                    instanceCount = ec2NodeConfigs.size
                    type = SpotInstanceType.OneTime
                    launchSpecification = specification
                }
                val spotRequestResponse = retryIfException { ec2Client.requestSpotInstances(spotRequestRequest) }
                val spotRequestId = spotRequestResponse.spotInstanceRequests?.mapNotNull { it.spotInstanceRequestId }
                var instanceIds = emptyList<String>()
                while (instanceIds.size != ec2NodeConfigs.size) {
                    delay(3000)
                    val describeRequest = DescribeSpotInstanceRequestsRequest {
                        spotInstanceRequestIds = spotRequestId
                    }
                    val describeRequestResponse = ec2Client.describeSpotInstanceRequests(describeRequest)
                    val spotInstanceRequest = describeRequestResponse.spotInstanceRequests
                        ?.filter { it.state == SpotInstanceState.Active }
                        ?.mapNotNull { it.instanceId }
                        ?: emptyList()
                    instanceIds = spotInstanceRequest
                    val failedSpotRequests = describeRequestResponse.spotInstanceRequests
                        ?.filter { it.state == SpotInstanceState.Failed }
                        ?: emptyList()
                    if (failedSpotRequests.isNotEmpty()) {
                        for ((ec2Config, instanceId) in ec2NodeConfigs zip instanceIds) {
                            ec2Config.instanceId = instanceId
                        }
                        throw RuntimeException("SpotRequest failed")
                    }
                }
                for ((ec2Config, instanceId) in ec2NodeConfigs zip instanceIds) {
                    ec2Config.instanceId = instanceId
                }
            }
    }

    suspend fun getEc2InstanceAddresses(ec2Configuration: Ec2Configuration) {
        val nodes = ec2Configuration.nodes
        val instanceCount = nodes.size
        val describeInstancesRequest = DescribeInstancesRequest {
            instanceIds = nodes.map { it.instanceId!! }
        }

        var isAllRunning = false
        while (!isAllRunning) {
            val describeInstancesResponse = retryIfException { ec2Client.describeInstances(describeInstancesRequest) }
            val reservations = describeInstancesResponse.reservations ?: emptyList()
            val runningInstances = reservations.flatMap { it.instances ?: emptyList() }
                .filter { it.state?.name == InstanceStateName.Running }
            if (runningInstances.size != instanceCount) {
                delay(3000)
            } else {
                isAllRunning = true
                val runningInstancesMap = runningInstances.associateBy { it.instanceId }
                nodes.forEach { node ->
                    val matchingResponse = runningInstancesMap[node.instanceId]
                    node.ipv4 = matchingResponse?.networkInterfaces?.get(0)?.privateIpAddress
                    node.ipv6 = matchingResponse?.ipv6Address
                }
            }
        }
        logger.info { "EC2 instances are running" }
    }

    suspend fun terminateEc2Instance(instances: List<NodeConfig>): List<String> {
        val instanceIdToDelete = instances.mapNotNull { it.instanceId }
        if (instanceIdToDelete.isNotEmpty()) {
            val terminateInstancesRequest = TerminateInstancesRequest {
                instanceIds = instanceIdToDelete
            }
            retryIfException { ec2Client.terminateInstances(terminateInstancesRequest) }
            logger.info { "Stopped instances $instances" }
            return instanceIdToDelete
        }
        return emptyList()
    }

    suspend fun deleteSecurityGroup(securityGroup: String?, instances: List<String>) {
        if (securityGroup != null) {
            if (instances.isNotEmpty()) {
                val describeInstancesRequest = DescribeInstancesRequest {
                    instanceIds = instances
                }
                while(true) {
                    val describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest)
                    val reservations = describeInstancesResponse.reservations ?: emptyList()
                    val terminatedInstances = reservations.flatMap { it.instances ?: emptyList() }
                        .filter { it.state?.name == InstanceStateName.Terminated }
                    if (terminatedInstances.size == instances.size) {
                        break
                    } else {
                        delay(15_000)
                    }
                }
            }

            val deleteRequest = DeleteSecurityGroupRequest {
                groupId = securityGroup
            }
            retryIfException { ec2Client.deleteSecurityGroup(deleteRequest) }
            logger.info { "Deleted security group $securityGroup" }
        }
    }

    suspend fun deleteSubnet(subnet: String?) {
        if (subnet != null) {
            val deleteRequest = DeleteSubnetRequest {
                subnetId = subnet
            }
            retryIfException { ec2Client.deleteSubnet(deleteRequest) }
            logger.info { "Deleted subnet $subnet" }
        }
    }

    suspend fun deleteInternetGateway(internetGateway: String?, vpc: String?) {
        if (internetGateway != null) {
            val detachRequest = DetachInternetGatewayRequest {
                internetGatewayId = internetGateway
                vpcId = vpc
            }
            retryIfException { ec2Client.detachInternetGateway(detachRequest) }
            logger.info { "Detached Internet gateway $internetGateway" }
            val deleteRequest = DeleteInternetGatewayRequest {
                internetGatewayId = internetGateway
            }
            retryIfException { ec2Client.deleteInternetGateway(deleteRequest) }
            logger.info { "Deleted Internet gateway $internetGateway" }
        }
    }

    suspend fun deleteVpc(vpc: String?) {
        if (vpc != null) {
            val deleteRequest = DeleteVpcRequest {
                vpcId = vpc
            }
            retryIfException { ec2Client.deleteVpc(deleteRequest) }
            logger.info { "Deleted VPC $vpc" }
        }
    }

    private suspend fun <T> retryIfException(action: suspend () -> T): T {
        var attempts = 0
        while (attempts < 3) {
            try {
                return action()
            } catch (e: Ec2Exception) {
                attempts++
                logger.warn { "Encountered Ec2Exception: ${e.message} Attempt $attempts of 3." }

                if (attempts >= 3) {
                    logger.error { "Encountered 3 Ec2Exceptions. Stopping execution." }
                    throw e
                }
            } catch (e: Exception) {
                logger.error { "An unexpected error occurred: ${e.message} Stopping execution." }
                throw e
            }
        }
        throw IllegalStateException("This code should not be executed.")
    }
}
