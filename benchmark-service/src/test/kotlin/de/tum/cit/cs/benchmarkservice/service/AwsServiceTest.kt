package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.*
import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import de.tum.cit.cs.benchmarkservice.model.NodeConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class AwsServiceTest {

    @MockkBean
    private lateinit var ec2Client: Ec2Client

    private lateinit var awsService: AwsService

    companion object {
        private const val VPC = "Vpc"
        private const val INTERNET_GATEWAY = "InternetGateway"
        private const val ROUTE_TABLE = "RouteTable"
        private const val SUBNET = "Subnet"
        private const val SECURITY_GROUP = "SecurityGroup"
        private const val KEY_PAIR_NAME = "aws-benchmark-private-key"
        private const val AVAILABILITY_ZONE = "us-east-1a"
        private const val CIDR_IPV4 = "10.1.0.0/16"
        private const val CIDR_IPV6 = "d044:05ab:f827:6800::/56"
    }

    @BeforeEach
    fun setUp() {
        awsService = AwsService(ec2Client, KEY_PAIR_NAME, VPC, CIDR_IPV4, CIDR_IPV6, AVAILABILITY_ZONE)
    }

    @Test
    fun `get InstanceTypInfo objects when multiple calls required and error occurs`() = runTest {
        // given
        val instanceTypeInfo1 = InstanceTypeInfo { instanceType = InstanceType.T3Nano }
        val instanceTypeInfo2 = InstanceTypeInfo { instanceType = InstanceType.T3Micro }
        val instanceTypeInfo3 = InstanceTypeInfo { instanceType = InstanceType.T3Small }

        coEvery {
            ec2Client.describeInstanceTypes(any<DescribeInstanceTypesRequest>())
        } returns DescribeInstanceTypesResponse {
            instanceTypes = listOf(instanceTypeInfo1, instanceTypeInfo2)
            nextToken = "token"
        } andThenThrows Ec2Exception() andThen DescribeInstanceTypesResponse {
            instanceTypes = listOf(instanceTypeInfo3)
            nextToken = null
        }

        // when
        val result = awsService.getInstancesFromAws()

        // then
        val expectedResult = listOf(instanceTypeInfo1, instanceTypeInfo2, instanceTypeInfo3)
        assertEquals(expectedResult, result)
        coVerify { ec2Client.describeInstanceTypes(any<DescribeInstanceTypesRequest>()) }
    }

    @Test
    fun `create vpc after error`() = runTest {
        // given
        val createRequestSlot = slot<CreateVpcRequest>()
        val modifyRequestSlot = mutableListOf<ModifyVpcAttributeRequest>()
        val associateRequestSlot = slot<AssociateVpcCidrBlockRequest>()
        coEvery {
            ec2Client.createVpc(capture(createRequestSlot))
        } throws Ec2Exception() andThen CreateVpcResponse { vpc = Vpc { vpcId = VPC } }

        coEvery {
            ec2Client.modifyVpcAttribute(capture(modifyRequestSlot))
        } throws Ec2Exception() andThen ModifyVpcAttributeResponse {} andThenThrows
                Ec2Exception() andThen ModifyVpcAttributeResponse { }

        coEvery {
            ec2Client.associateVpcCidrBlock(capture(associateRequestSlot))
        } throws Ec2Exception() andThen AssociateVpcCidrBlockResponse {
            ipv6CidrBlockAssociation = VpcIpv6CidrBlockAssociation { ipv6CidrBlock = CIDR_IPV6 }
        }

        // when
        awsService = AwsService(ec2Client, KEY_PAIR_NAME, null, CIDR_IPV4, null, AVAILABILITY_ZONE)
        awsService.createVpc()

        // then
        val capturedCreateRequest = createRequestSlot.captured
        assertEquals(CIDR_IPV4, capturedCreateRequest.cidrBlock)

        val firstCapturedModifyRequest = modifyRequestSlot[0]
        assertEquals(VPC, firstCapturedModifyRequest.vpcId)
        assertEquals(true, firstCapturedModifyRequest.enableDnsSupport!!.value)

        val secondCapturedModifyRequest = modifyRequestSlot[2]
        assertEquals(VPC, secondCapturedModifyRequest.vpcId)
        assertEquals(true, secondCapturedModifyRequest.enableDnsHostnames!!.value)

        val capturedAssociateRequest = associateRequestSlot.captured
        assertEquals(VPC, capturedAssociateRequest.vpcId)
        assertEquals(true, capturedAssociateRequest.amazonProvidedIpv6CidrBlock)

        coVerify(exactly = 2) { ec2Client.createVpc(any<CreateVpcRequest>()) }
        coVerify(exactly = 4) { ec2Client.modifyVpcAttribute(any<ModifyVpcAttributeRequest>()) }
        coVerify(exactly = 2) { ec2Client.associateVpcCidrBlock(any<AssociateVpcCidrBlockRequest>()) }
    }

    @Test
    fun `create vpc after waiting for IPv6`() = runTest {
        // given
        val describeRequestSlot = slot<DescribeVpcsRequest>()
        coEvery {
            ec2Client.createVpc(any<CreateVpcRequest>())
        } returns CreateVpcResponse { vpc = Vpc { vpcId = VPC } }

        coEvery {
            ec2Client.modifyVpcAttribute(any<ModifyVpcAttributeRequest>())
        } returns ModifyVpcAttributeResponse { }

        coEvery {
            ec2Client.associateVpcCidrBlock(any<AssociateVpcCidrBlockRequest>())
        } returns AssociateVpcCidrBlockResponse {
            ipv6CidrBlockAssociation = null
        }

        coEvery {
            ec2Client.describeVpcs(capture(describeRequestSlot))
        } returns DescribeVpcsResponse {
            vpcs = emptyList()
        } andThen DescribeVpcsResponse {
            vpcs = listOf(Vpc {
                ipv6CidrBlockAssociationSet = listOf(
                    VpcIpv6CidrBlockAssociation { ipv6CidrBlock = CIDR_IPV6 }
                )
            })
        }

        // when
        awsService = AwsService(ec2Client, KEY_PAIR_NAME, null, CIDR_IPV4, null, AVAILABILITY_ZONE)
        awsService.createVpc()

        // then
        val capturedDescribeRequest = describeRequestSlot.captured
        assertEquals(1, capturedDescribeRequest.vpcIds!!.size)
        assertEquals(VPC, capturedDescribeRequest.vpcIds!![0])
        assertEquals(1, capturedDescribeRequest.filters!!.size)
        assertEquals("vpc-id", capturedDescribeRequest.filters!![0].name)
        assertEquals(1, capturedDescribeRequest.filters!![0].values!!.size)
        assertEquals(VPC, capturedDescribeRequest.filters!![0].values!![0])

        coVerify(exactly = 1) { ec2Client.createVpc(any<CreateVpcRequest>()) }
        coVerify(exactly = 2) { ec2Client.modifyVpcAttribute(any<ModifyVpcAttributeRequest>()) }
        coVerify(exactly = 2) { ec2Client.describeVpcs(any<DescribeVpcsRequest>()) }
    }

    @Test
    fun `create internet gateway after error`() = runTest {
        // given
        val attachRequestSlot = slot<AttachInternetGatewayRequest>()
        coEvery {
            ec2Client.createInternetGateway(any<CreateInternetGatewayRequest>())
        } throws Ec2Exception() andThen CreateInternetGatewayResponse {
            internetGateway = InternetGateway { internetGatewayId = INTERNET_GATEWAY }
        }

        coEvery {
            ec2Client.attachInternetGateway(capture(attachRequestSlot))
        } throws Ec2Exception() andThen AttachInternetGatewayResponse {}

        // when
        awsService.createInternetGateway()

        // then
        val capturedAttachRequest = attachRequestSlot.captured
        assertEquals(VPC, capturedAttachRequest.vpcId)
        assertEquals(INTERNET_GATEWAY, capturedAttachRequest.internetGatewayId)

        coVerify(exactly = 2) { ec2Client.createInternetGateway(any<CreateInternetGatewayRequest>()) }
        coVerify(exactly = 2) { ec2Client.attachInternetGateway(any<AttachInternetGatewayRequest>()) }
    }

    @Test
    fun `configure route table after error`() = runTest {
        // given
        coEvery {
            ec2Client.createInternetGateway(any<CreateInternetGatewayRequest>())
        } returns CreateInternetGatewayResponse {
            internetGateway = InternetGateway { internetGatewayId = INTERNET_GATEWAY }
        }

        coEvery {
            ec2Client.attachInternetGateway(any<AttachInternetGatewayRequest>())
        } returns AttachInternetGatewayResponse {}

        val describeRequestSlot = slot<DescribeRouteTablesRequest>()
        val createRouteRequestSlot = slot<CreateRouteRequest>()
        coEvery {
            ec2Client.describeRouteTables(capture(describeRequestSlot))
        } throws Ec2Exception() andThen DescribeRouteTablesResponse {
            routeTables = listOf(RouteTable { routeTableId = ROUTE_TABLE })
        }

        coEvery {
            ec2Client.createRoute(capture(createRouteRequestSlot))
        } throws Ec2Exception() andThen CreateRouteResponse {}


        // when
        awsService.createInternetGateway()
        awsService.configureRouteTable()

        // then
        val capturedDescribeRequest = describeRequestSlot.captured
        assertEquals(1, capturedDescribeRequest.filters!!.size)
        assertEquals("vpc-id", capturedDescribeRequest.filters!![0].name)
        assertEquals(1, capturedDescribeRequest.filters!![0].values!!.size)
        assertEquals(VPC, capturedDescribeRequest.filters!![0].values!![0])

        val capturedCreateRouteRequest = createRouteRequestSlot.captured
        assertEquals(ROUTE_TABLE, capturedCreateRouteRequest.routeTableId)
        assertEquals("::/0", capturedCreateRouteRequest.destinationIpv6CidrBlock)
        assertEquals(INTERNET_GATEWAY, capturedCreateRouteRequest.gatewayId)

        coVerify(exactly = 2) { ec2Client.describeRouteTables(any<DescribeRouteTablesRequest>()) }
        coVerify(exactly = 2) { ec2Client.createRoute(any<CreateRouteRequest>()) }
    }

    @Test
    fun `create subnet after error`() = runTest {
        // given
        val createRequestSlot = slot<CreateSubnetRequest>()
        val modifyRequestSlot = slot<ModifySubnetAttributeRequest>()
        coEvery {
            ec2Client.createSubnet(capture(createRequestSlot))
        } throws Ec2Exception() andThen CreateSubnetResponse { subnet = Subnet { subnetId = SUBNET } }

        coEvery {
            ec2Client.modifySubnetAttribute(capture(modifyRequestSlot))
        } throws Ec2Exception() andThen ModifySubnetAttributeResponse { }

        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory", emptyList(),
            ipv4Cidr = CIDR_IPV4, ipv6Cidr = CIDR_IPV6
        )

        // when
        awsService.init()
        awsService.createSubnet(ec2Configuration)

        // then
        assertEquals(SUBNET, ec2Configuration.subnetId)

        val capturedCreateRequest = createRequestSlot.captured
        assertEquals(VPC, capturedCreateRequest.vpcId)
        assertEquals("10.1.1.0/24", capturedCreateRequest.cidrBlock)
        assertEquals("d044:05ab:f827:6801::/64", capturedCreateRequest.ipv6CidrBlock)
        assertEquals(AVAILABILITY_ZONE, capturedCreateRequest.availabilityZoneId)

        val capturedModifyRequest = modifyRequestSlot.captured
        assertEquals(SUBNET, capturedModifyRequest.subnetId)
        assertEquals(true, capturedModifyRequest.assignIpv6AddressOnCreation!!.value)

        coVerify(exactly = 2) { ec2Client.createSubnet(any<CreateSubnetRequest>()) }
        coVerify(exactly = 2) { ec2Client.modifySubnetAttribute(any<ModifySubnetAttributeRequest>()) }
    }

    @Test
    fun `create security group after error`() = runTest {
        // given
        val createRequestSlot = slot<CreateSecurityGroupRequest>()
        val ingressRequestSlot = slot<AuthorizeSecurityGroupIngressRequest>()
        coEvery {
            ec2Client.createSecurityGroup(capture(createRequestSlot))
        } throws Ec2Exception() andThen CreateSecurityGroupResponse { groupId = SECURITY_GROUP }

        coEvery {
            ec2Client.authorizeSecurityGroupIngress(capture(ingressRequestSlot))
        } throws Ec2Exception() andThen AuthorizeSecurityGroupIngressResponse { }

        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory", emptyList(),
            ipv4Cidr = CIDR_IPV4, ipv6Cidr = CIDR_IPV6
        )

        // when
        awsService.createSecurityGroup(ec2Configuration)

        // then
        assertEquals(SECURITY_GROUP, ec2Configuration.securityGroupId)

        val capturedCreateRequest = createRequestSlot.captured
        assertEquals(VPC, capturedCreateRequest.vpcId)
        assertEquals("benchmark-id", capturedCreateRequest.groupName)
        assertEquals("benchmark-id", capturedCreateRequest.description)

        val capturedIngressRequest = ingressRequestSlot.captured
        assertEquals(SECURITY_GROUP, capturedIngressRequest.groupId)
        assertEquals(3, capturedIngressRequest.ipPermissions!!.size)
        assertEquals("-1", capturedIngressRequest.ipPermissions!![0].ipProtocol)
        assertEquals(1, capturedIngressRequest.ipPermissions!![0].ipRanges!!.size)
        assertEquals(CIDR_IPV4, capturedIngressRequest.ipPermissions!![0].ipRanges!![0].cidrIp)

        assertEquals("-1", capturedIngressRequest.ipPermissions!![1].ipProtocol)
        assertEquals(1, capturedIngressRequest.ipPermissions!![1].ipv6Ranges!!.size)
        assertEquals(CIDR_IPV6, capturedIngressRequest.ipPermissions!![1].ipv6Ranges!![0].cidrIpv6)

        assertEquals("Tcp", capturedIngressRequest.ipPermissions!![2].ipProtocol)
        assertEquals(1, capturedIngressRequest.ipPermissions!![2].ipv6Ranges!!.size)
        assertEquals("::/0", capturedIngressRequest.ipPermissions!![2].ipv6Ranges!![0].cidrIpv6)
        assertEquals(22, capturedIngressRequest.ipPermissions!![2].fromPort)
        assertEquals(22, capturedIngressRequest.ipPermissions!![2].toPort)

        coVerify(exactly = 2) { ec2Client.createSecurityGroup(any<CreateSecurityGroupRequest>()) }
        coVerify(exactly = 2) { ec2Client.authorizeSecurityGroupIngress(any<AuthorizeSecurityGroupIngressRequest>()) }
    }

    @Test
    fun `start EC2 instances after error`() = runTest {
        val capturedCreateRequests = mutableListOf<RequestSpotInstancesRequest>()
        val capturedDescribeRequests = mutableListOf<DescribeSpotInstanceRequestsRequest>()
        coEvery {
            ec2Client.requestSpotInstances(capture(capturedCreateRequests))
        } throws Ec2Exception() andThen RequestSpotInstancesResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest { spotInstanceRequestId = "requestId1" },
                SpotInstanceRequest { spotInstanceRequestId = "requestId2" },
            )
        } andThen RequestSpotInstancesResponse {
            spotInstanceRequests = listOf(SpotInstanceRequest { spotInstanceRequestId = "requestId3" })
        } andThen RequestSpotInstancesResponse {
            spotInstanceRequests = listOf(SpotInstanceRequest { spotInstanceRequestId = "requestId4" })
        }

        coEvery {
            ec2Client.describeSpotInstanceRequests(capture(capturedDescribeRequests))
        } throws Ec2Exception() andThen DescribeSpotInstanceRequestsResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest { state = SpotInstanceState.Open },
                SpotInstanceRequest {
                    state = SpotInstanceState.Active
                    instanceId = "id2"
                },
            )
        } andThen DescribeSpotInstanceRequestsResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest {
                    state = SpotInstanceState.Active
                    instanceId = "id1"
                },
                SpotInstanceRequest {
                    state = SpotInstanceState.Active
                    instanceId = "id2"
                },
            )
        } andThen DescribeSpotInstanceRequestsResponse {
            spotInstanceRequests = listOf(SpotInstanceRequest {
                state = SpotInstanceState.Active
                instanceId = "id3"
            })
        } andThen DescribeSpotInstanceRequestsResponse {
            spotInstanceRequests = listOf(SpotInstanceRequest {
                state = SpotInstanceState.Active
                instanceId = "id4"
            })
        }

        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory",
            listOf(
                NodeConfig(
                    1, null, "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    2, null, "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    3, null, "t2.micro", "image2",
                    null, null, null, null, null
                ),
                NodeConfig(
                    4, null, "t3.micro", "image",
                    null, null, null, null, null
                )
            ),
            securityGroupId = SECURITY_GROUP, subnetId = SUBNET
        )

        // when
        awsService.startEc2Instance(ec2Configuration)

        // then
        assertEquals("id1", ec2Configuration.nodes[0].instanceId)
        assertEquals("id2", ec2Configuration.nodes[1].instanceId)
        assertEquals("id3", ec2Configuration.nodes[2].instanceId)
        assertEquals("id4", ec2Configuration.nodes[3].instanceId)

        assertEquals(4, capturedCreateRequests.size)
        assertEquals(SpotInstanceType.OneTime, capturedCreateRequests[0].type)
        assertEquals(2, capturedCreateRequests[0].instanceCount)
        assertEquals("image", capturedCreateRequests[0].launchSpecification!!.imageId)
        assertEquals(InstanceType.T2Micro, capturedCreateRequests[0].launchSpecification!!.instanceType)
        assertEquals(KEY_PAIR_NAME, capturedCreateRequests[0].launchSpecification!!.keyName)
        assertEquals(SUBNET, capturedCreateRequests[0].launchSpecification!!.subnetId)
        assertEquals(1, capturedCreateRequests[0].launchSpecification!!.securityGroupIds!!.size)
        assertEquals(SECURITY_GROUP, capturedCreateRequests[0].launchSpecification!!.securityGroupIds!![0])

        assertEquals(SpotInstanceType.OneTime, capturedCreateRequests[2].type)
        assertEquals(1, capturedCreateRequests[2].instanceCount)
        assertEquals("image2", capturedCreateRequests[2].launchSpecification!!.imageId)
        assertEquals(InstanceType.T2Micro, capturedCreateRequests[2].launchSpecification!!.instanceType)
        assertEquals(KEY_PAIR_NAME, capturedCreateRequests[2].launchSpecification!!.keyName)
        assertEquals(SUBNET, capturedCreateRequests[2].launchSpecification!!.subnetId)
        assertEquals(1, capturedCreateRequests[2].launchSpecification!!.securityGroupIds!!.size)
        assertEquals(SECURITY_GROUP, capturedCreateRequests[2].launchSpecification!!.securityGroupIds!![0])

        assertEquals(SpotInstanceType.OneTime, capturedCreateRequests[3].type)
        assertEquals(1, capturedCreateRequests[3].instanceCount)
        assertEquals("image", capturedCreateRequests[3].launchSpecification!!.imageId)
        assertEquals(InstanceType.T3Micro, capturedCreateRequests[3].launchSpecification!!.instanceType)
        assertEquals(KEY_PAIR_NAME, capturedCreateRequests[3].launchSpecification!!.keyName)
        assertEquals(SUBNET, capturedCreateRequests[3].launchSpecification!!.subnetId)
        assertEquals(1, capturedCreateRequests[3].launchSpecification!!.securityGroupIds!!.size)
        assertEquals(SECURITY_GROUP, capturedCreateRequests[3].launchSpecification!!.securityGroupIds!![0])

        assertEquals(5, capturedDescribeRequests.size)
        assertEquals(listOf("requestId1", "requestId2"), capturedDescribeRequests[0].spotInstanceRequestIds)
        assertEquals(listOf("requestId1", "requestId2"), capturedDescribeRequests[1].spotInstanceRequestIds)
        assertEquals(listOf("requestId1", "requestId2"), capturedDescribeRequests[2].spotInstanceRequestIds)
        assertEquals(listOf("requestId3"), capturedDescribeRequests[3].spotInstanceRequestIds)
        assertEquals(listOf("requestId4"), capturedDescribeRequests[4].spotInstanceRequestIds)

        coVerify(exactly = 4) { ec2Client.requestSpotInstances(any<RequestSpotInstancesRequest>()) }
        coVerify(exactly = 5) { ec2Client.describeSpotInstanceRequests(any<DescribeSpotInstanceRequestsRequest>()) }
    }

    @Test
    fun `return exception when starting EC2 instances fails`() = runTest {
        val capturedCreateRequests = mutableListOf<RequestSpotInstancesRequest>()
        val capturedDescribeRequests = mutableListOf<DescribeSpotInstanceRequestsRequest>()
        coEvery {
            ec2Client.requestSpotInstances(capture(capturedCreateRequests))
        } returns RequestSpotInstancesResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest { spotInstanceRequestId = "requestId1" },
                SpotInstanceRequest { spotInstanceRequestId = "requestId2" },
            )
        }

        coEvery {
            ec2Client.describeSpotInstanceRequests(capture(capturedDescribeRequests))
        } returns DescribeSpotInstanceRequestsResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest { state = SpotInstanceState.Open },
                SpotInstanceRequest {
                    state = SpotInstanceState.Active
                    instanceId = "id2"
                },
            )
        } andThen DescribeSpotInstanceRequestsResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest {
                    state = SpotInstanceState.Failed
                },
                SpotInstanceRequest {
                    state = SpotInstanceState.Active
                    instanceId = "id2"
                },
            )
        }

        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory",
            listOf(
                NodeConfig(
                    1, null, "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    2, null, "t2.micro", "image",
                    null, null, null, null, null
                )
            ),
            securityGroupId = SECURITY_GROUP, subnetId = SUBNET
        )

        // when
        runCatching {
            awsService.startEc2Instance(ec2Configuration)
        }.onFailure {
            assertThat(it).isInstanceOf(RuntimeException::class.java)
        }

        // then
        assertEquals("id2", ec2Configuration.nodes[0].instanceId)
        assertEquals(null, ec2Configuration.nodes[1].instanceId)

        assertEquals(1, capturedCreateRequests.size)

        assertEquals(2, capturedDescribeRequests.size)
        assertEquals(listOf("requestId1", "requestId2"), capturedDescribeRequests[0].spotInstanceRequestIds)
        assertEquals(listOf("requestId1", "requestId2"), capturedDescribeRequests[1].spotInstanceRequestIds)

        coVerify(exactly = 1) { ec2Client.requestSpotInstances(any<RequestSpotInstancesRequest>()) }
        coVerify(exactly = 2) { ec2Client.describeSpotInstanceRequests(any<DescribeSpotInstanceRequestsRequest>()) }
    }

    @Test
    fun `get EC2 instances IP addresses`() = runTest {
        val describeRequestSlot = slot<DescribeInstancesRequest>()
        coEvery {
            ec2Client.describeInstances(capture(describeRequestSlot))
        } throws Ec2Exception() andThen DescribeInstancesResponse {
            reservations = listOf(
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id1"
                            state = InstanceState { name = InstanceStateName.Pending }
                        },
                        Instance {
                            instanceId = "id2"
                            state = InstanceState { name = InstanceStateName.Running }
                            ipv6Address = "10::1"
                            networkInterfaces = listOf(InstanceNetworkInterface { privateIpAddress = "10.0.0.1" })
                        }
                    )
                },
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id3"
                            state = InstanceState { name = InstanceStateName.Pending }
                        },
                    )
                }
            )
        } andThen DescribeInstancesResponse {
            reservations = listOf(
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id1"
                            state = InstanceState { name = InstanceStateName.Running }
                            ipv6Address = "10::2"
                            networkInterfaces = listOf(InstanceNetworkInterface { privateIpAddress = "10.0.0.2" })
                        },
                        Instance {
                            instanceId = "id2"
                            state = InstanceState { name = InstanceStateName.Running }
                            ipv6Address = "10::1"
                            networkInterfaces = listOf(InstanceNetworkInterface { privateIpAddress = "10.0.0.1" })
                        }
                    )
                },
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id3"
                            state = InstanceState { name = InstanceStateName.Running }
                            ipv6Address = "10::3"
                            networkInterfaces = listOf(InstanceNetworkInterface { privateIpAddress = "10.0.0.3" })
                        },
                    )
                }
            )
        }
        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory",
            listOf(
                NodeConfig(
                    1, "id1", "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    2, "id2", "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    2, "id3", "t2.micro", "image2",
                    null, null, null, null, null
                )
            ),
            securityGroupId = SECURITY_GROUP, subnetId = SUBNET
        )

        // when
        awsService.getEc2InstanceAddresses(ec2Configuration)

        // then
        val capturedRequest = describeRequestSlot.captured
        assertEquals("10.0.0.2", ec2Configuration.nodes[0].ipv4)
        assertEquals("10::2", ec2Configuration.nodes[0].ipv6)
        assertEquals("10.0.0.1", ec2Configuration.nodes[1].ipv4)
        assertEquals("10::1", ec2Configuration.nodes[1].ipv6)
        assertEquals("10.0.0.3", ec2Configuration.nodes[2].ipv4)
        assertEquals("10::3", ec2Configuration.nodes[2].ipv6)
        assertEquals(listOf("id1", "id2", "id3"), capturedRequest.instanceIds)
        coVerify(exactly = 3) { ec2Client.describeInstances(any<DescribeInstancesRequest>()) }
    }

    @Test
    fun `terminate instances after error`() = runTest {
        // given
        val terminateRequestSlot = slot<TerminateInstancesRequest>()
        coEvery {
            ec2Client.terminateInstances(capture(terminateRequestSlot))
        } throws Ec2Exception() andThen TerminateInstancesResponse { }
        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory",
            listOf(
                NodeConfig(
                    1, "id1", "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    2, null, "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    2, "id3", "t2.micro", "image2",
                    null, null, null, null, null
                )
            )
        )

        // when
        val result = awsService.terminateEc2Instance(ec2Configuration)

        // then
        assertEquals(listOf("id1", "id3"), result)
        val capturedRequest = terminateRequestSlot.captured
        assertEquals(listOf("id1", "id3"), capturedRequest.instanceIds)
        coVerify(exactly = 2) { ec2Client.terminateInstances(any<TerminateInstancesRequest>()) }
    }

    @Test
    fun `terminate instances when list is empty`() = runTest {
        // given
        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory",
            listOf(
                NodeConfig(
                    1, null, "t2.micro", "image",
                    null, null, null, null, null
                ),
            ),
        )

        // when
        val result = awsService.terminateEc2Instance(ec2Configuration)

        // then
        assertEquals(emptyList<String>(), result)
        coVerify(exactly = 0) { ec2Client.terminateInstances(any<TerminateInstancesRequest>()) }
    }

    @Test
    fun `delete security group after error`() = runTest {
        // given
        val describeRequestSlot = slot<DescribeInstancesRequest>()
        val deleteRequestSlot = slot<DeleteSecurityGroupRequest>()
        coEvery {
            ec2Client.describeInstances(capture(describeRequestSlot))
        } throws Ec2Exception() andThen DescribeInstancesResponse {
            reservations = listOf(
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id1"
                            state = InstanceState { name = InstanceStateName.ShuttingDown }
                        },
                        Instance {
                            instanceId = "id2"
                            state = InstanceState { name = InstanceStateName.Terminated }
                        }
                    )
                },
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id3"
                            state = InstanceState { name = InstanceStateName.ShuttingDown }
                        },
                    )
                })
        } andThen DescribeInstancesResponse {
            reservations = listOf(
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id1"
                            state = InstanceState { name = InstanceStateName.Terminated }
                        },
                        Instance {
                            instanceId = "id2"
                            state = InstanceState { name = InstanceStateName.Terminated }
                        }
                    )
                },
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id3"
                            state = InstanceState { name = InstanceStateName.Terminated }
                        },
                    )
                })
        }

        coEvery {
            ec2Client.deleteSecurityGroup(capture(deleteRequestSlot))
        } throws Ec2Exception() andThen DeleteSecurityGroupResponse { }

        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory",
            listOf(
                NodeConfig(
                    1, "id1", "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    2, "id2", "t2.micro", "image",
                    null, null, null, null, null
                ),
                NodeConfig(
                    4, "id3", "t2.micro", "image2",
                    null, null, null, null, null
                )
            ),
            securityGroupId = SECURITY_GROUP
        )
        val instanceIds = listOf("id1", "id2", "id3")

        // when
        awsService.deleteSecurityGroup(ec2Configuration, instanceIds)

        // then
        val capturedDescribeRequest = describeRequestSlot.captured
        assertEquals(instanceIds, capturedDescribeRequest.instanceIds)

        val capturedDeleteRequest = deleteRequestSlot.captured
        assertEquals(SECURITY_GROUP, capturedDeleteRequest.groupId)

        coVerify(exactly = 3) { ec2Client.describeInstances(any<DescribeInstancesRequest>()) }
        coVerify(exactly = 2) { ec2Client.deleteSecurityGroup(any<DeleteSecurityGroupRequest>()) }
    }

    @Test
    fun `do not delete security group when it is null`() = runTest {
        // given
        val ec2Configuration = Ec2Configuration("id", "testDirectory", emptyList())

        // when
        awsService.deleteSecurityGroup(ec2Configuration, emptyList())

        // then
        coVerify(exactly = 0) { ec2Client.deleteSecurityGroup(any<DeleteSecurityGroupRequest>()) }
    }

    @Test
    fun `delete subnet after error`() = runTest {
        // given
        val deleteSubnetRequestSlot = slot<DeleteSubnetRequest>()
        coEvery {
            ec2Client.deleteSubnet(capture(deleteSubnetRequestSlot))
        } throws Ec2Exception() andThen DeleteSubnetResponse { }
        val ec2Configuration = Ec2Configuration(
            "id", "testDirectory", emptyList(),
            subnetId = SUBNET, ipv4Cidr = "10.0.1.0/24"
        )

        // when
        awsService.init()
        awsService.deleteSubnet(ec2Configuration)

        // then
        val capturedRequest = deleteSubnetRequestSlot.captured
        assertEquals(SUBNET, capturedRequest.subnetId)
        coVerify(exactly = 2) { ec2Client.deleteSubnet(any<DeleteSubnetRequest>()) }
    }

    @Test
    fun `do not delete subnet when it is null`() = runTest {
        // given
        val ec2Configuration = Ec2Configuration("id", "testDirectory", emptyList())

        // when
        awsService.deleteSubnet(ec2Configuration)

        // then
        coVerify(exactly = 0) { ec2Client.deleteSubnet(any<DeleteSubnetRequest>()) }
    }

    @Test
    fun `delete internet gateway after error`() = runTest {
        // given
        coEvery {
            ec2Client.createInternetGateway(any<CreateInternetGatewayRequest>())
        } returns CreateInternetGatewayResponse {
            internetGateway = InternetGateway { internetGatewayId = INTERNET_GATEWAY }
        }

        coEvery {
            ec2Client.attachInternetGateway(any<AttachInternetGatewayRequest>())
        } returns AttachInternetGatewayResponse {}

        val detachRequestSlot = slot<DetachInternetGatewayRequest>()
        val deleteRequestSlot = slot<DeleteInternetGatewayRequest>()
        coEvery {
            ec2Client.detachInternetGateway(capture(detachRequestSlot))
        } throws Ec2Exception() andThen DetachInternetGatewayResponse { }

        coEvery {
            ec2Client.deleteInternetGateway(capture(deleteRequestSlot))
        } throws Ec2Exception() andThen DeleteInternetGatewayResponse { }

        // when
        awsService.createInternetGateway()
        awsService.deleteInternetGateway()

        // then
        val capturedDetachRequest = detachRequestSlot.captured
        assertEquals(VPC, capturedDetachRequest.vpcId)
        assertEquals(INTERNET_GATEWAY, capturedDetachRequest.internetGatewayId)
        val capturedDeleteRequest = deleteRequestSlot.captured
        assertEquals(INTERNET_GATEWAY, capturedDeleteRequest.internetGatewayId)
        coVerify(exactly = 2) { ec2Client.detachInternetGateway(any<DetachInternetGatewayRequest>()) }
        coVerify(exactly = 2) { ec2Client.deleteInternetGateway(any<DeleteInternetGatewayRequest>()) }
    }

    @Test
    fun `do not delete internet gateway when it is null`() = runTest {
        // when
        awsService.deleteInternetGateway()

        // then
        coVerify(exactly = 0) { ec2Client.deleteInternetGateway(any<DeleteInternetGatewayRequest>()) }
    }

    @Test
    fun `delete vpc after error`() = runTest {
        // given
        val deleteVpcRequestSlot = slot<DeleteVpcRequest>()
        coEvery {
            ec2Client.deleteVpc(capture(deleteVpcRequestSlot))
        } throws Ec2Exception() andThen DeleteVpcResponse { }

        // when
        awsService.deleteVpc()

        // then
        val capturedRequest = deleteVpcRequestSlot.captured
        assertEquals(VPC, capturedRequest.vpcId)
        coVerify(exactly = 2) { ec2Client.deleteVpc(any<DeleteVpcRequest>()) }
    }

    @Test
    fun `do not vpc when it is null`() = runTest {
        // given
        awsService = AwsService(ec2Client, KEY_PAIR_NAME, null, CIDR_IPV4, null, AVAILABILITY_ZONE)

        // when
        awsService.deleteVpc()

        // then
        coVerify(exactly = 0) { ec2Client.deleteVpc(any<DeleteVpcRequest>()) }
    }

    @Test
    fun `test if exception rethrow if occurs 3 times`() = runTest {
        // given
        coEvery {
            ec2Client.deleteVpc(any<DeleteVpcRequest>())
        } throwsMany listOf(Ec2Exception(), Ec2Exception(), Ec2Exception())

        // when
        runCatching {
            awsService.deleteVpc()
        }.onFailure {
            assertThat(it).isInstanceOf(Ec2Exception::class.java)
        }

        // then
        coVerify(exactly = 3) { ec2Client.deleteVpc(any<DeleteVpcRequest>()) }
    }

    @Test
    fun `test if exception rethrow if it is not Ec2Exception`() = runTest {
        // given
        coEvery {
            ec2Client.deleteVpc(any<DeleteVpcRequest>())
        } throws RuntimeException()

        // when
        runCatching {
            awsService.deleteVpc()
        }.onFailure {
            assertThat(it).isInstanceOf(RuntimeException::class.java)
        }

        // then
        coVerify(exactly = 1) { ec2Client.deleteVpc(any<DeleteVpcRequest>()) }
    }
}
