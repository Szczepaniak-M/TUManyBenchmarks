package de.tum.cit.cs.benchmarkservice.model

data class Ec2Configuration(
    val benchmarkRunId: String,
    val directory: String,
    val nodes: List<NodeConfig>,
    var ipv4Cidr: String? = null,
    var ipv6Cidr: String? = null,
    var subnetId: String? = null,
    var securityGroupId: String? = null,
)

data class NodeConfig(
    val nodeId: Int,
    var instanceId: String? = null,
    val instanceType: String,
    val image: String,
    val ansibleConfiguration: String?,
    val benchmarkCommand: String?,
    val outputCommand: String?,
    var ipv4: String? = null,
    var ipv6: String? = null,
)
