package de.tum.cit.cs.webpage.service

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.DescribeSpotPriceHistoryResponse
import aws.sdk.kotlin.services.ec2.model.InstanceType
import aws.sdk.kotlin.services.ec2.model.SpotPrice
import aws.sdk.kotlin.services.pricing.PricingClient
import aws.sdk.kotlin.services.pricing.model.GetProductsResponse
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
class Ec2PriceServiceTest {

    private lateinit var ec2PriceService: Ec2PriceService

    @MockkBean
    private lateinit var pricingClient: PricingClient

    @MockkBean
    private lateinit var ec2Client: Ec2Client

    @BeforeEach
    fun setup() {
        ec2PriceService = Ec2PriceService(ec2Client, pricingClient)
    }

    @Test
    fun `get prices if they exists after update`() = runBlocking {
        // Given
        val onDemandPrice1 = """
            {
                "product": { "attributes": { "instanceType": "t2.micro" } },
                "terms": {
                    "OnDemand": {
                        "randomString.randomString": {
                            "priceDimensions": {
                                "randomString.randomString.randomString": {
                                    "pricePerUnit": { "USD": "0.1" }
                                }
                            }
                        }
                    }
                }
            }
        """


        val onDemandPrice2 = """
            {
                "product": { "attributes": { "instanceType": "t3.micro" } },
                "terms": {
                    "OnDemand": {
                        "randomString.randomString": {
                            "priceDimensions": {
                                "randomString.randomString.randomString": {
                                    "pricePerUnit": { "USD": "0.2" }
                                }
                            }
                        }
                    }
                }
            }
        """
        val response1 = GetProductsResponse {
            priceList = listOf(onDemandPrice1)
            nextToken = "token"
        }
        val response2 = GetProductsResponse {
            priceList = listOf(onDemandPrice2)
            nextToken = ""
        }

        val spotRequestPrice1 = DescribeSpotPriceHistoryResponse {
            spotPriceHistory = listOf(
                SpotPrice {
                    instanceType = InstanceType.T2Micro
                    spotPrice = "0.01"
                },
                SpotPrice {
                    instanceType = InstanceType.T3Micro
                    spotPrice = "0.02"
                }
            )
            nextToken = "token"
        }
        val spotRequestPrice2 = DescribeSpotPriceHistoryResponse {
            spotPriceHistory = listOf(
                SpotPrice {
                    instanceType = InstanceType.T2Micro
                    spotPrice = "0.009"
                },
                SpotPrice {
                    instanceType = InstanceType.T3Micro
                    spotPrice = "0.021"
                }
            )
            nextToken = ""
        }

        coEvery { pricingClient.getProducts(any()) } returnsMany listOf(response1, response2)
        coEvery { ec2Client.describeSpotPriceHistory(any()) } returnsMany listOf(spotRequestPrice1, spotRequestPrice2)

        // When
        ec2PriceService.updateAllEc2Prices()

        // Then
        assertEquals(BigDecimal("0.1"), ec2PriceService.getOnDemandPrice("t2.micro"))
        assertEquals(BigDecimal("0.009"), ec2PriceService.getSpotPrice("t2.micro"))
        assertEquals(BigDecimal("0.2"), ec2PriceService.getOnDemandPrice("t3.micro"))
        assertEquals(BigDecimal("0.02"), ec2PriceService.getSpotPrice("t3.micro"))
        assertEquals(BigDecimal.ZERO, ec2PriceService.getOnDemandPrice("t4.micro"))
        assertEquals(BigDecimal.ZERO, ec2PriceService.getSpotPrice("t4.micro"))
    }
}
