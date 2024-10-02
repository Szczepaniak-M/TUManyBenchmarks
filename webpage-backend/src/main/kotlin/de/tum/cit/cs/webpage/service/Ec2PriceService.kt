package de.tum.cit.cs.webpage.service

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.ec2.model.DescribeSpotPriceHistoryRequest
import aws.sdk.kotlin.services.pricing.PricingClient
import aws.sdk.kotlin.services.pricing.model.Filter
import aws.sdk.kotlin.services.pricing.model.FilterType
import aws.sdk.kotlin.services.pricing.model.GetProductsRequest
import aws.smithy.kotlin.runtime.time.Instant
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@Service
@Profile("!test")
@EnableScheduling
class Ec2PriceService(
    private val ec2Client: Ec2Client,
    private val pricingClient: PricingClient
) {
    private val logger = KotlinLogging.logger {}
    private val objectMapper = jacksonObjectMapper()
    private val cacheOnDemand = Caffeine.newBuilder()
        .expireAfterWrite(65, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build<String, BigDecimal>()
    private val cacheSpot = Caffeine.newBuilder()
        .expireAfterWrite(65, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build<String, BigDecimal>()

    fun getOnDemandPrice(price: String): BigDecimal {
        return cacheOnDemand.get(price, fun(_: String) = BigDecimal.ZERO)
    }

    fun getSpotPrice(price: String): BigDecimal {
        return cacheSpot.get(price, fun(_: String) = BigDecimal.ZERO)
    }

    @PostConstruct
    @Scheduled(cron = "0 0 * * * *")
    fun updateAllEc2Prices() = runBlocking {
        logger.info { "Starting updating prices from AWS" }
        updateOnDemandEc2Prices()
        updateSpotEc2Prices()
        logger.info { "Finishing updating prices from AWS" }
    }

    private suspend fun updateOnDemandEc2Prices() {
        logger.info { "Starting downloading data about on-demand instances prices from AWS" }
        var counter = 0
        var nextToken: String? = null
        do {
            val request = GetProductsRequest {
                serviceCode = "AmazonEC2"
                filters = listOf(
                    Filter {
                        field = "location"
                        value = "US East (N. Virginia)" // US-EAST-1
                        type = FilterType.TermMatch
                    },
                    Filter {
                        field = "preInstalledSw"
                        value = "NA"
                        type = FilterType.TermMatch
                    },
                    Filter {
                        field = "operatingSystem"
                        value = "Linux"
                        type = FilterType.TermMatch
                    },
                    Filter {
                        field = "capacitystatus"
                        value = "Used"
                        type = FilterType.TermMatch
                    },
                    Filter {
                        field = "tenancy"
                        value = "shared"
                        type = FilterType.TermMatch
                    },
                )
                if (nextToken != null) {
                    this.nextToken = nextToken
                }
            }

            val response = pricingClient.getProducts(request)
            logger.debug { "Downloaded information about ${response.priceList?.size ?: 0} prices" }
            counter += response.priceList?.size ?: 0
            response.priceList?.forEach { priceJson ->
                val priceObject = objectMapper.readTree(priceJson)
                cacheOnDemandPrice(priceObject)
            }
            nextToken = response.nextToken
        } while (nextToken?.isNotEmpty() == true)
        logger.info {
            "Finished downloading data about on-demand instances prices from AWS." +
                    "Downloaded $counter prices."
        }
    }

    private fun cacheOnDemandPrice(priceObject: JsonNode) {
        val instanceType = priceObject["product"]?.get("attributes")?.get("instanceType")?.asText()
        val onDemand = priceObject["terms"]?.get("OnDemand")
        onDemand?.let {
            val price = extractOnDemandPrice(it)
            cacheOnDemand.put(instanceType, price)
        }
    }

    private fun extractOnDemandPrice(onDemandNode: JsonNode): BigDecimal {
        onDemandNode.fields().forEach { (_, termNode) ->
            val priceDimensions = termNode["priceDimensions"]
            priceDimensions?.fields()?.forEach { (_, dimensionNode) ->
                val price = dimensionNode["pricePerUnit"]?.get("USD")?.asText()
                return BigDecimal(price)
            }
        }
        return BigDecimal.ZERO
    }

    private suspend fun updateSpotEc2Prices() {
        logger.info { "Starting downloading data about spot instances prices from AWS" }
        var counter = 0
        var nextToken: String? = null
        val newCache = mutableMapOf<String, BigDecimal>()
        do {
            val request = DescribeSpotPriceHistoryRequest {
                startTime = Instant.now()
                endTime = Instant.now()
                productDescriptions = listOf("Linux/UNIX")
                if (nextToken != null) {
                    this.nextToken = nextToken
                }
            }
            val response = ec2Client.describeSpotPriceHistory(request)
            logger.debug { "Downloaded information about ${response.spotPriceHistory?.size ?: 0} prices" }
            response.spotPriceHistory?.forEach { spotPrice ->
                newCache.merge(spotPrice.instanceType!!.value,
                    BigDecimal(spotPrice.spotPrice),
                    fun(prev, new: BigDecimal) = minOf(prev, new)
                )
            }
            counter += response.spotPriceHistory?.size ?: 0
            nextToken = response.nextToken
        } while (nextToken?.isNotEmpty() == true)
        cacheSpot.putAll(newCache)
        logger.info {
            "Finished downloading data about spot instances prices from AWS." +
                    "Downloaded $counter prices from multiple AZ."
        }
    }
}