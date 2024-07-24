package de.tum.cit.cs.webpage.model

data class ExplorerResponse(
    val totalQueries: Int,
    val successfulQueries: Int,
    val results: List<String>,
    val error: String?,
)