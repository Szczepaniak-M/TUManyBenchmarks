package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.*
import aws.smithy.kotlin.runtime.time.Instant
import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import de.tum.cit.cs.benchmarkservice.model.NodeConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Service
class AwsService(
    private val ec2Client: Ec2Client,
    @Value("\${aws.ec2.private-key.name}")
    private val keyPairName: String,
    @Value("\${aws.ec2.network.vpcId:#{null}}")
    private var vpc: String?,
    @Value("\${aws.ec2.network.ipv4Cidr}")
    private val ipv4Cidr: String,
    @Value("\${aws.ec2.network.ipv6Cidr:#{null}}")
    private var ipv6Cidr: String?,
    @Value("\${aws.ec2.network.availability-zone}")
    private val networkAvailabilityZone: String,
) {
    private val logger = KotlinLogging.logger {}
    private lateinit var subnetworkService: SubnetworkService
    private var internetGateway: String? = null

    @PostConstruct
    fun init() = runBlocking {
        if (vpc == null && ipv6Cidr == null) {
            logger.info { "Starting creating resources for local execution " }
            createVpc()
            createInternetGateway()
            configureRouteTable()
            logger.info { "Created resources for local execution " }
        } else if (vpc == null || ipv6Cidr == null) {
            throw IllegalStateException("VPC ID and IPv6 must both be specified or nulls")
        }
        subnetworkService = SubnetworkService(ipv4Cidr, ipv6Cidr!!)
    }

    @PreDestroy
    fun destroy() = runBlocking {
        if (internetGateway != null) {
            logger.info { "Starting deleting resources for local execution " }
            deleteInternetGateway()
            deleteVpc()
            logger.info { "Deleted resources for local execution " }
        }
    }

    suspend fun getInstancesFromAws(): List<InstanceTypeInfo> {
        logger.info { "Starting downloading data about instances from AWS" }
        var nextTokenFromResponse: String? = ""
        var request = DescribeInstanceTypesRequest {}
        val instanceTypeInfos = mutableListOf<InstanceTypeInfo>()
        do {
            val response = retryIfException {
                ec2Client.describeInstanceTypes(request)
            }
            val newInstances = response.instanceTypes
            logger.debug { "Downloaded information about ${newInstances?.size} instances" }
            instanceTypeInfos.addAll(newInstances ?: emptyList())
            nextTokenFromResponse = response.nextToken
            request = DescribeInstanceTypesRequest { nextToken = nextTokenFromResponse }
        } while (nextTokenFromResponse?.isNotEmpty() == true)
        logger.info {
            "Finished downloading data about instances from AWS. " +
                    "Downloaded information about ${instanceTypeInfos.size} instances."
        }
        return instanceTypeInfos
    }

    suspend fun createVpc() {
        val createVpcRequest = CreateVpcRequest { cidrBlock = ipv4Cidr }
        val createVpcResponse = retryIfException { ec2Client.createVpc(createVpcRequest) }
        this.vpc = createVpcResponse.vpc?.vpcId
        val dnsSupportRequest = ModifyVpcAttributeRequest {
            vpcId = vpc
            enableDnsSupport = AttributeBooleanValue { value = true }
        }
        retryIfException { ec2Client.modifyVpcAttribute(dnsSupportRequest) }
        val enableDnsHostnamesRequest = ModifyVpcAttributeRequest {
            vpcId = vpc
            enableDnsHostnames = AttributeBooleanValue { value = true }
        }
        retryIfException { ec2Client.modifyVpcAttribute(enableDnsHostnamesRequest) }
        val ipv6CidrRequest = AssociateVpcCidrBlockRequest {
            vpcId = vpc
            amazonProvidedIpv6CidrBlock = true
        }
        val ipv6CidrResponse = retryIfException { ec2Client.associateVpcCidrBlock(ipv6CidrRequest) }
        this.ipv6Cidr = ipv6CidrResponse.ipv6CidrBlockAssociation?.ipv6CidrBlock
        awaitForIpv6IfNotAssigned()
        logger.info { "Created VPC ${this.vpc}" }
    }

    private suspend fun awaitForIpv6IfNotAssigned() {
        while (this.ipv6Cidr == null) {
            delay(3000)
            val describeVpcRequest = DescribeVpcsRequest {
                vpcIds = listOf(vpc!!)
                filters = listOf(
                    Filter {
                        name = "vpc-id"
                        values = listOf(vpc!!)
                    }
                )
            }
            val describeVpcResponse = ec2Client.describeVpcs(describeVpcRequest)
            this.ipv6Cidr = describeVpcResponse.vpcs
                ?.firstOrNull()
                ?.ipv6CidrBlockAssociationSet
                ?.firstOrNull()
                ?.ipv6CidrBlock
        }
    }

    suspend fun createInternetGateway() {
        val createIgwRequest = CreateInternetGatewayRequest {}
        val createIgwResponse = retryIfException { ec2Client.createInternetGateway(createIgwRequest) }
        this.internetGateway = createIgwResponse.internetGateway?.internetGatewayId
        val attachIgwRequest = AttachInternetGatewayRequest {
            vpcId = vpc
            internetGatewayId = internetGateway
        }
        retryIfException { ec2Client.attachInternetGateway(attachIgwRequest) }
        logger.info { "Created Internet Gateway $internetGateway" }
    }

    suspend fun configureRouteTable() {
        val describeRouteTablesRequest = DescribeRouteTablesRequest {
            filters = listOf(Filter {
                name = "vpc-id"
                values = listOf(vpc!!)
            })
        }
        val describeRouteTablesResponse = retryIfException { ec2Client.describeRouteTables(describeRouteTablesRequest) }
        val routeTable = describeRouteTablesResponse.routeTables?.firstOrNull()?.routeTableId
        val createIpv6RouteRequest = CreateRouteRequest {
            routeTableId = routeTable
            destinationIpv6CidrBlock = "::/0"
            gatewayId = internetGateway
        }
        retryIfException { ec2Client.createRoute(createIpv6RouteRequest) }
        logger.info { "Configured RouteTable $routeTable" }
    }

    suspend fun createSubnet(ec2Configuration: Ec2Configuration) {
        subnetworkService.assignAvailableSubnetwork(ec2Configuration)
        val createSubnetRequest = CreateSubnetRequest {
            vpcId = vpc
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
        logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Created Subnet ${ec2Configuration.subnetId}" }
    }

    suspend fun createSecurityGroup(ec2Configuration: Ec2Configuration) {
        val createRequest = CreateSecurityGroupRequest {
            groupName = "benchmark-${ec2Configuration.benchmarkRunId}"
            description = "benchmark-${ec2Configuration.benchmarkRunId}"
            vpcId = vpc
        }
        val createSecurityGroupResponse = retryIfException { ec2Client.createSecurityGroup(createRequest) }
        ec2Configuration.securityGroupId = createSecurityGroupResponse.groupId
        val ipv4PermissionIngress = IpPermission {
            ipProtocol = "-1"
            ipRanges = listOf(IpRange { cidrIp = ec2Configuration.ipv4Cidr })
        }
        val ipv6PermissionIngress = IpPermission {
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
            ipPermissions = listOf(ipv4PermissionIngress, ipv6PermissionIngress, sshIpv6PermissionIngress)
        }
        retryIfException { ec2Client.authorizeSecurityGroupIngress(ingressRequest) }
        logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Created Security Group ${ec2Configuration.securityGroupId}" }
    }

    suspend fun startEc2Instance(ec2Configuration: Ec2Configuration) {
        logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Starting ${ec2Configuration.nodes.size} EC2 instances" }
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
                val spotInstanceResponse = retryIfException {
                    val spotInstanceRequest = RequestSpotInstancesRequest {
                        instanceCount = ec2NodeConfigs.size
                        type = SpotInstanceType.OneTime
                        launchSpecification = specification
                        validUntil = Instant.now().plus(1.toDuration(DurationUnit.MINUTES))
                    }
                    ec2Client.requestSpotInstances(spotInstanceRequest)
                }
                val spotRequestIds = spotInstanceResponse.spotInstanceRequests?.mapNotNull { it.spotInstanceRequestId }
                val instanceIds = awaitForInstanceIds(ec2NodeConfigs, spotRequestIds, ec2Configuration.benchmarkRunId)
                for ((ec2Config, instanceId) in ec2NodeConfigs zip instanceIds) {
                    ec2Config.instanceId = instanceId
                }
            }
    }

    private suspend fun awaitForInstanceIds(
        ec2NodeConfigs: List<NodeConfig>,
        spotRequestIds: List<String>?,
        benchmarkRunId: String
    ): List<String> {
        var instanceIds = emptyList<String>()
        while (instanceIds.size != ec2NodeConfigs.size) {
            delay(3000)
            val describeSpotRequestsRequest = DescribeSpotInstanceRequestsRequest {
                spotInstanceRequestIds = spotRequestIds
            }
            val describeRequestResponse =
                retryIfException { ec2Client.describeSpotInstanceRequests(describeSpotRequestsRequest) }
            instanceIds = describeRequestResponse.spotInstanceRequests
                ?.filter { it.state == SpotInstanceState.Active }
                ?.mapNotNull { it.instanceId }
                ?: emptyList()
            val failedSpotRequests = describeRequestResponse.spotInstanceRequests
                ?.filter {
                    it.state == SpotInstanceState.Failed ||
                            it.state == SpotInstanceState.Closed ||
                            it.state == SpotInstanceState.Cancelled
                }
                ?: emptyList()
            if (failedSpotRequests.isNotEmpty()) {
                for ((ec2Config, instanceId) in ec2NodeConfigs zip instanceIds) {
                    ec2Config.instanceId = instanceId
                }
                logger.error { "Benchmark $benchmarkRunId: One of the Spot Instance requests failed. Stopping benchmark" }
                throw RuntimeException("SpotRequest failed for benchmark $benchmarkRunId")
            }
        }
        return instanceIds
    }

    suspend fun getEc2InstanceAddresses(ec2Configuration: Ec2Configuration) {
        val nodes = ec2Configuration.nodes
        val instanceCount = nodes.size
        val describeInstancesRequest = DescribeInstancesRequest {
            instanceIds = nodes.map { it.instanceId!! }
        }
        var runningInstances = emptyList<Instance>()
        while (runningInstances.size != instanceCount) {
            delay(3000)
            val describeInstancesResponse = retryIfException { ec2Client.describeInstances(describeInstancesRequest) }
            val reservations = describeInstancesResponse.reservations ?: emptyList()
            runningInstances = reservations.flatMap { it.instances ?: emptyList() }
                .filter { it.state?.name == InstanceStateName.Running }
        }
        val runningInstancesMap = runningInstances.associateBy { it.instanceId }
        nodes.forEach { node ->
            val matchingResponse = runningInstancesMap[node.instanceId]
            node.ipv4 = matchingResponse?.networkInterfaces?.get(0)?.privateIpAddress
            node.ipv6 = matchingResponse?.ipv6Address
        }
        logger.info {
            "Benchmark ${ec2Configuration.benchmarkRunId}: EC2 instances are ready to connect. " +
                    "Instance IDs: ${nodes.map { it.instanceId }}"
        }
    }

    suspend fun terminateEc2Instance(ec2Configuration: Ec2Configuration): List<String> {
        val instanceIdToDelete = ec2Configuration.nodes.mapNotNull { it.instanceId }
        if (instanceIdToDelete.isNotEmpty()) {
            val terminateInstancesRequest = TerminateInstancesRequest {
                instanceIds = instanceIdToDelete
            }
            retryIfException { ec2Client.terminateInstances(terminateInstancesRequest) }
            logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Terminated instances $instanceIdToDelete" }
            return instanceIdToDelete
        }
        return emptyList()
    }

    suspend fun deleteSecurityGroup(ec2Configuration: Ec2Configuration, instances: List<String>) {
        if (ec2Configuration.securityGroupId != null) {
            if (instances.isNotEmpty()) {
                val describeInstancesRequest = DescribeInstancesRequest {
                    instanceIds = instances
                }
                var terminatedInstances = emptyList<Instance>()
                while (terminatedInstances.size != instances.size) {
                    delay(15_000)
                    val describeInstancesResponse =
                        retryIfException { ec2Client.describeInstances(describeInstancesRequest) }
                    val reservations = describeInstancesResponse.reservations ?: emptyList()
                    terminatedInstances = reservations.flatMap { it.instances ?: emptyList() }
                        .filter { it.state?.name == InstanceStateName.Terminated }
                }
            }
            val deleteRequest = DeleteSecurityGroupRequest {
                groupId = ec2Configuration.securityGroupId
            }
            retryIfException { ec2Client.deleteSecurityGroup(deleteRequest) }
            logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Deleted Security Group ${ec2Configuration.securityGroupId}" }
        }
    }

    suspend fun deleteSubnet(ec2Configuration: Ec2Configuration) {
        if (ec2Configuration.subnetId != null) {
            val deleteRequest = DeleteSubnetRequest {
                subnetId = ec2Configuration.subnetId
            }
            retryIfException { ec2Client.deleteSubnet(deleteRequest) }
            subnetworkService.releaseSubnetwork(ec2Configuration)
            logger.info { "Benchmark ${ec2Configuration.benchmarkRunId}: Deleted Subnet ${ec2Configuration.subnetId}" }
        }
    }

    suspend fun deleteInternetGateway() {
        if (internetGateway != null) {
            val detachRequest = DetachInternetGatewayRequest {
                internetGatewayId = internetGateway
                vpcId = vpc
            }
            retryIfException { ec2Client.detachInternetGateway(detachRequest) }
            val deleteRequest = DeleteInternetGatewayRequest {
                internetGatewayId = internetGateway
            }
            retryIfException { ec2Client.deleteInternetGateway(deleteRequest) }
            logger.info { "Deleted Internet Gateway $internetGateway" }
        }
    }

    suspend fun deleteVpc() {
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
                if (e.message == "Max spot instance count exceeded") {
                    logger.warn { "Encountered 'Max spot instance count exceeded' exception. Waiting before next try" }
                    delay(30_000)
                } else {
                    attempts++
                    logger.warn { "Encountered Ec2Exception: ${e.message} Attempt $attempts of 3." }

                    if (attempts >= 3) {
                        logger.error { "Encountered 3 Ec2Exceptions. Stopping execution." }
                        throw e
                    }
                    delay(1000)
                }
            } catch (e: Exception) {
                logger.error { "An unexpected error occurred: ${e.message} Stopping execution." }
                throw e
            }
        }
        throw IllegalStateException("This code should not be executed.")
    }
}
