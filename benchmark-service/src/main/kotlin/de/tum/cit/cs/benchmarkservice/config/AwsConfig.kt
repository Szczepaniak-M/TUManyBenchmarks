package de.tum.cit.cs.benchmarkservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2AsyncClient

@Configuration
class AwsConfig {
    @Bean
    fun ec2AsyncClient(): Ec2AsyncClient {
        return Ec2AsyncClient.builder()
            .region(Region.US_EAST_1)
            .build()
    }
}