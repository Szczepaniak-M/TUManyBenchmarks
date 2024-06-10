package de.tum.cit.cs.benchmarkservice.config

import aws.sdk.kotlin.services.ec2.Ec2Client
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
}