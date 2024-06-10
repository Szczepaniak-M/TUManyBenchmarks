package de.tum.cit.cs.benchmarkservice.service

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.*
import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.benchmarkservice.model.Instance
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
        private const val SUBNET = "Subnet"
        private const val SECURITY_GROUP = "SecurityGroup"
        private const val AMI_X86_64 = "ami-04b70fa74e45c3917"
        private const val AMI_ARM = "ami-0eac975a54dfee8cb"
        private const val KEY_PAIR_NAME = "aws-benchmark-private-key"
        private const val AVAILABILITY_ZONE = "us-east-1a"
        private const val CIDR = "10.0.0.0/28"
    }

    @BeforeEach
    fun setUp() {
        awsService = AwsService(ec2Client)
        awsService.amiX86_64 = AMI_X86_64
        awsService.amiArm = AMI_ARM
        awsService.keyPairName = KEY_PAIR_NAME
        awsService.networkAvailabilityZone = AVAILABILITY_ZONE
        awsService.networkCidr = CIDR
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
        val modifyRequestSlot = slot<ModifyVpcAttributeRequest>()
        coEvery {
            ec2Client.createVpc(capture(createRequestSlot))
        } throws Ec2Exception() andThen CreateVpcResponse { vpc = Vpc { vpcId = VPC } }
        coEvery {
            ec2Client.modifyVpcAttribute(capture(modifyRequestSlot))
        } throws Ec2Exception() andThen ModifyVpcAttributeResponse { }

        // when
        val result = awsService.createVpc()

        // then
        assertEquals(VPC, result)
        val capturedCreateRequest = createRequestSlot.captured
        assertEquals(CIDR, capturedCreateRequest.cidrBlock)
        val capturedModifyRequest = modifyRequestSlot.captured
        assertEquals(VPC, capturedModifyRequest.vpcId)
        assertEquals(true, capturedModifyRequest.enableDnsSupport!!.value)
        assertEquals(true, capturedModifyRequest.enableDnsHostnames!!.value)
        coVerify(exactly = 2) { ec2Client.createVpc(any<CreateVpcRequest>()) }
        coVerify(exactly = 2) { ec2Client.modifyVpcAttribute(any<ModifyVpcAttributeRequest>()) }
    }

    @Test
    fun `create subnet after error`() = runTest {
        // given
        val requestSlot = slot<CreateSubnetRequest>()
        coEvery {
            ec2Client.createSubnet(capture(requestSlot))
        } throws Ec2Exception() andThen CreateSubnetResponse { subnet = Subnet { subnetId = SUBNET } }

        // when
        val result = awsService.createSubnet(VPC)

        // then
        assertEquals(SUBNET, result)
        val capturedRequest = requestSlot.captured
        assertEquals(VPC, capturedRequest.vpcId)
        assertEquals(CIDR, capturedRequest.cidrBlock)
        assertEquals(AVAILABILITY_ZONE, capturedRequest.availabilityZoneId)
        coVerify(exactly = 2) { ec2Client.createSubnet(any<CreateSubnetRequest>()) }
    }

    @Test
    fun `create security group after error`() = runTest {
        // given
        val createRequestSlot = slot<CreateSecurityGroupRequest>()
        val ingressRequestSlot = slot<AuthorizeSecurityGroupIngressRequest>()
        val egressRequestSlot = slot<AuthorizeSecurityGroupEgressRequest>()
        coEvery {
            ec2Client.createSecurityGroup(capture(createRequestSlot))
        } throws Ec2Exception() andThen CreateSecurityGroupResponse { groupId = SECURITY_GROUP }
        coEvery {
            ec2Client.authorizeSecurityGroupIngress(capture(ingressRequestSlot))
        } throws Ec2Exception() andThen AuthorizeSecurityGroupIngressResponse { }
        coEvery {
            ec2Client.authorizeSecurityGroupEgress(capture(egressRequestSlot))
        } throws Ec2Exception() andThen AuthorizeSecurityGroupEgressResponse { }

        // when
        val result = awsService.createSecurityGroup(VPC)

        // then
        assertEquals(SECURITY_GROUP, result)
        val capturedCreateRequest = createRequestSlot.captured
        assertEquals(VPC, capturedCreateRequest.vpcId)
        val capturedIngressRequest = ingressRequestSlot.captured
        assertEquals(SECURITY_GROUP, capturedIngressRequest.groupId)
        assertEquals(0, capturedIngressRequest.ipPermissions!![0].fromPort)
        assertEquals(65535, capturedIngressRequest.ipPermissions!![0].toPort)
        assertEquals("-1", capturedIngressRequest.ipPermissions!![0].ipProtocol)
        assertEquals(CIDR, capturedIngressRequest.ipPermissions!![0].ipRanges!![0].cidrIp)
        val capturedEgressRequest = egressRequestSlot.captured
        assertEquals(SECURITY_GROUP, capturedEgressRequest.groupId)
        assertEquals(0, capturedEgressRequest.ipPermissions!![0].fromPort)
        assertEquals(65535, capturedEgressRequest.ipPermissions!![0].toPort)
        assertEquals("-1", capturedEgressRequest.ipPermissions!![0].ipProtocol)
        assertEquals("0.0.0.0/0", capturedEgressRequest.ipPermissions!![0].ipRanges!![0].cidrIp)
        coVerify(exactly = 2) { ec2Client.createSecurityGroup(any<CreateSecurityGroupRequest>()) }
        coVerify(exactly = 2) { ec2Client.authorizeSecurityGroupIngress(any<AuthorizeSecurityGroupIngressRequest>()) }
        coVerify(exactly = 2) { ec2Client.authorizeSecurityGroupEgress(any<AuthorizeSecurityGroupEgressRequest>()) }
    }

    @Test
    fun `start EC2 ARM instances after error`() = runTest {
        val instance = Instance("id", "t2.micro", listOf("arm64"))
        val count = 3
        val requestSpotInstancesRequestSlot = slot<RequestSpotInstancesRequest>()
        coEvery {
            ec2Client.requestSpotInstances(capture(requestSpotInstancesRequestSlot))
        } throws Ec2Exception() andThen RequestSpotInstancesResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest { instanceId = "id1" },
                SpotInstanceRequest { instanceId = "id2" },
                SpotInstanceRequest { instanceId = "id3" }
            )
        }

        // when
        val result = awsService.startEc2Instance(instance, count, SUBNET, SECURITY_GROUP)

        // then
        assertEquals(listOf("id1", "id2", "id3"), result)
        val capturedRequest = requestSpotInstancesRequestSlot.captured
        assertEquals(count, capturedRequest.instanceCount)
        assertEquals(SpotInstanceType.OneTime, capturedRequest.type)
        assertEquals(AMI_ARM, capturedRequest.launchSpecification!!.imageId)
        assertEquals(InstanceType.T2Micro, capturedRequest.launchSpecification!!.instanceType)
        assertEquals(KEY_PAIR_NAME, capturedRequest.launchSpecification!!.keyName)
        assertEquals(SUBNET, capturedRequest.launchSpecification!!.subnetId)
        assertEquals(SECURITY_GROUP, capturedRequest.launchSpecification!!.securityGroups!![0])
        coVerify(exactly = 2) { ec2Client.requestSpotInstances(any<RequestSpotInstancesRequest>()) }
    }

    @Test
    fun `start EC2 x86_64 instances after error`() = runTest {
        val instance = Instance("id", "t2.micro", listOf("i386", "x86_64"))
        val count = 3
        val requestSpotInstancesRequestSlot = slot<RequestSpotInstancesRequest>()
        coEvery {
            ec2Client.requestSpotInstances(capture(requestSpotInstancesRequestSlot))
        } throws Ec2Exception() andThen RequestSpotInstancesResponse {
            spotInstanceRequests = listOf(
                SpotInstanceRequest { instanceId = "id1" },
                SpotInstanceRequest { instanceId = "id2" },
                SpotInstanceRequest { instanceId = "id3" }
            )
        }

        // when
        val result = awsService.startEc2Instance(instance, count, SUBNET, SECURITY_GROUP)

        // then
        assertEquals(listOf("id1", "id2", "id3"), result)
        val capturedRequest = requestSpotInstancesRequestSlot.captured
        assertEquals(count, capturedRequest.instanceCount)
        assertEquals(SpotInstanceType.OneTime, capturedRequest.type)
        assertEquals(AMI_X86_64, capturedRequest.launchSpecification!!.imageId)
        assertEquals(InstanceType.T2Micro, capturedRequest.launchSpecification!!.instanceType)
        assertEquals(KEY_PAIR_NAME, capturedRequest.launchSpecification!!.keyName)
        assertEquals(SUBNET, capturedRequest.launchSpecification!!.subnetId)
        assertEquals(SECURITY_GROUP, capturedRequest.launchSpecification!!.securityGroups!![0])
        coVerify(exactly = 2) { ec2Client.requestSpotInstances(any<RequestSpotInstancesRequest>()) }
    }

    @Test
    fun `get EC2 instances Public DNS`() = runTest {
        val describeInstancesRequestSlot = slot<DescribeInstancesRequest>()
        coEvery {
            ec2Client.describeInstances(capture(describeInstancesRequestSlot))
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
                            publicDnsName = "publicDomain2"
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
                            publicDnsName = "publicDomain1"
                        },
                        Instance {
                            instanceId = "id2"
                            state = InstanceState { name = InstanceStateName.Running }
                            publicDnsName = "publicDomain2"
                        }
                    )
                },
                Reservation {
                    instances = listOf(
                        Instance {
                            instanceId = "id3"
                            state = InstanceState { name = InstanceStateName.Running }
                            publicDnsName = "publicDomain3"
                        },
                    )
                }
            )
        }
        val listOfInstanceIds = listOf("id1", "id2", "id3")

        // when
        val result = awsService.getEc2InstanceAddresses(listOfInstanceIds)

        // then
        assertEquals(listOf("publicDomain1", "publicDomain2", "publicDomain3"), result)
        val capturedRequest = describeInstancesRequestSlot.captured
        assertEquals(listOfInstanceIds, capturedRequest.instanceIds)
        coVerify(exactly = 3) { ec2Client.describeInstances(any<DescribeInstancesRequest>()) }
    }

    @Test
    fun `terminate instances after error`() = runTest {
        // given
        val terminateInstancesRequestSlot = slot<TerminateInstancesRequest>()
        coEvery {
            ec2Client.terminateInstances(capture(terminateInstancesRequestSlot))
        } throws Ec2Exception() andThen TerminateInstancesResponse { }

        // when
        awsService.terminateEc2Instance(listOf("id1", "id2", "id3"))

        // then
        val capturedRequest = terminateInstancesRequestSlot.captured
        assertEquals(listOf("id1", "id2", "id3"), capturedRequest.instanceIds)
        coVerify(exactly = 2) { ec2Client.terminateInstances(any<TerminateInstancesRequest>()) }
    }

    @Test
    fun `delete security group after error`() = runTest {
        // given
        val deleteSecurityGroupRequestSlot = slot<DeleteSecurityGroupRequest>()
        coEvery {
            ec2Client.deleteSecurityGroup(capture(deleteSecurityGroupRequestSlot))
        } throws Ec2Exception() andThen DeleteSecurityGroupResponse { }

        // when
        awsService.deleteSecurityGroup(SECURITY_GROUP)

        // then
        val capturedRequest = deleteSecurityGroupRequestSlot.captured
        assertEquals(SECURITY_GROUP, capturedRequest.groupId)
        coVerify(exactly = 2) { ec2Client.deleteSecurityGroup(any<DeleteSecurityGroupRequest>()) }
    }

    @Test
    fun `delete subnet after error`() = runTest {
        // given
        val deleteSubnetRequestSlot = slot<DeleteSubnetRequest>()
        coEvery {
            ec2Client.deleteSubnet(capture(deleteSubnetRequestSlot))
        } throws Ec2Exception() andThen DeleteSubnetResponse { }

        // when
        awsService.deleteSubnet(SUBNET)

        // then
        val capturedRequest = deleteSubnetRequestSlot.captured
        assertEquals(SUBNET, capturedRequest.subnetId)
        coVerify(exactly = 2) { ec2Client.deleteSubnet(any<DeleteSubnetRequest>()) }
    }

    @Test
    fun `delete vpc after error`() = runTest {
        // given
        val deleteVpcRequestSlot = slot<DeleteVpcRequest>()
        coEvery {
            ec2Client.deleteVpc(capture(deleteVpcRequestSlot))
        } throws Ec2Exception() andThen DeleteVpcResponse { }

        // when
        awsService.deleteVpc(VPC)

        // then
        val capturedRequest = deleteVpcRequestSlot.captured
        assertEquals(VPC, capturedRequest.vpcId)
        coVerify(exactly = 2) { ec2Client.deleteVpc(any<DeleteVpcRequest>()) }
    }

    @Test
    fun `test if exception rethrow if occurs 3 times`() = runTest {
        // given
        coEvery {
            ec2Client.deleteVpc(any<DeleteVpcRequest>())
        } throwsMany listOf(Ec2Exception(), Ec2Exception(), Ec2Exception())

        // when
        runCatching {
            awsService.deleteVpc(VPC)
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
            awsService.deleteVpc(VPC)
        }.onFailure {
            assertThat(it).isInstanceOf(RuntimeException::class.java)
        }

        // then
        coVerify(exactly = 1) { ec2Client.deleteVpc(any<DeleteVpcRequest>()) }
    }
}
