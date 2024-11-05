package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Ec2Configuration
import java.util.concurrent.ConcurrentLinkedQueue

class SubnetworkService(
    val ipv4Network: String,
    val ipv6Network: String
) {
    // leave a few subnets free to create a subnets for the service in different AZs
    private val availableSubnetworks = ConcurrentLinkedQueue<Int>((6..254).toList())

    fun assignAvailableSubnetwork(ec2Configuration: Ec2Configuration) {
        val subnetId = availableSubnetworks.poll()
        ec2Configuration.ipv4Cidr = getIpv4Subnet(subnetId)
        ec2Configuration.ipv6Cidr = getIpv6Subnet(subnetId)
    }

    fun releaseSubnetwork(ec2Configuration: Ec2Configuration) {
        val subnetId = ec2Configuration.ipv4Cidr?.split(".")?.get(2)?.toInt()
        availableSubnetworks.add(subnetId)
    }

    private fun getIpv4Subnet(subnetId: Int?): String {
        val ipv4NetworkSplit = ipv4Network.split(".").toMutableList()
        ipv4NetworkSplit[2] = subnetId.toString()
        ipv4NetworkSplit[3] = ipv4NetworkSplit[3].replace("/16", "/24")
        return ipv4NetworkSplit.joinToString(separator = ".")
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getIpv6Subnet(subnetId: Int?): String {
        val subnetIdHex = subnetId?.toHexString()?.removePrefix("000000")
        return ipv6Network.replace("00::/56", "$subnetIdHex::/64")
    }
}