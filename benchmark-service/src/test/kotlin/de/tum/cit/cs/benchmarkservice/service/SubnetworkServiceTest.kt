package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubnetworkServiceTest {

    private lateinit var subnetworkService: SubnetworkService
    private lateinit var ec2Configuration: Ec2Configuration

    @BeforeEach
    fun setUp() {
        subnetworkService = SubnetworkService("10.0.0.0/16", "d044:05ab:f827:6800::/56")
        ec2Configuration = Ec2Configuration("", "", emptyList())
    }

    @Test
    fun `test assignAvailableSubnetwork should assign correct IPv4 and IPv6 CIDR`() {
        // when
        subnetworkService.assignAvailableSubnetwork(ec2Configuration)

        // then
        assertEquals("10.0.1.0/24", ec2Configuration.ipv4Cidr)
        assertEquals("d044:05ab:f827:6801::/64", ec2Configuration.ipv6Cidr)
    }

    @Test
    fun `test assignAvailableSubnetwork should use next available subnet`() {
        // given
        subnetworkService.assignAvailableSubnetwork(ec2Configuration)

        // when
        subnetworkService.assignAvailableSubnetwork(ec2Configuration)

        // then
        assertEquals("10.0.2.0/24", ec2Configuration.ipv4Cidr)
        assertEquals("d044:05ab:f827:6802::/64", ec2Configuration.ipv6Cidr)
    }

    @Test
    fun `test releaseSubnetwork should add subnet back to pool`() {
        // given
        repeat(254) {
            subnetworkService.assignAvailableSubnetwork(ec2Configuration)
        }

        // when
        subnetworkService.releaseSubnetwork(ec2Configuration)
        subnetworkService.assignAvailableSubnetwork(ec2Configuration)

        // then
        assertEquals("10.0.254.0/24", ec2Configuration.ipv4Cidr)
        assertEquals("d044:05ab:f827:68fe::/64", ec2Configuration.ipv6Cidr)
    }
}
