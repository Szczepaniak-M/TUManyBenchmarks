package de.tum.cit.cs.webpage.config

import aws.sdk.kotlin.services.ec2.Ec2Client
import aws.sdk.kotlin.services.pricing.PricingClient
import aws.sdk.kotlin.services.pricing.PricingClient.Companion.invoke
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AwsConfig {
    @Bean
    fun ec2Client(): Ec2Client {
        return Ec2Client {
            region = "us-east-1"
        }
    }

    @Bean
    fun pricingClient(): PricingClient {
        return PricingClient {
            region = "us-east-1"
        }
    }
}
