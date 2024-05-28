package de.tum.cit.cs.benchmarkservice.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

import software.amazon.awssdk.services.ec2.Ec2AsyncClient
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo
import java.util.concurrent.CompletableFuture

@ExtendWith(SpringExtension::class)
class AwsServiceTest {

    @MockkBean
    private lateinit var ec2AsyncClient: Ec2AsyncClient

    @Test
    fun `get InstanceTypInfo objects when multiple calls required`() = runTest {
        // given
        val awsService = AwsService(ec2AsyncClient)

        val instanceTypeInfo1 = InstanceTypeInfo.builder().instanceType("t3.nano").build()
        val instanceTypeInfo2 = InstanceTypeInfo.builder().instanceType("t3.micro").build()
        val instanceTypeInfo3 = InstanceTypeInfo.builder().instanceType("t3.small").build()

        every {
            ec2AsyncClient.describeInstanceTypes(any<DescribeInstanceTypesRequest>())
        } returnsMany listOf(
            CompletableFuture.completedFuture(
                DescribeInstanceTypesResponse.builder()
                    .instanceTypes(instanceTypeInfo1, instanceTypeInfo2)
                    .nextToken("token")
                    .build()
            ),
            CompletableFuture.completedFuture(
                DescribeInstanceTypesResponse.builder()
                    .instanceTypes(instanceTypeInfo3)
                    .nextToken(null)
                    .build()
            )
        )

        // when
        val result = awsService.getInstancesFromAws()

        // then
        val expectedResult = listOf(instanceTypeInfo1, instanceTypeInfo2, instanceTypeInfo3)
        assertEquals(expectedResult, result)
        verify { ec2AsyncClient.describeInstanceTypes(any<DescribeInstanceTypesRequest>()) }
    }
}
