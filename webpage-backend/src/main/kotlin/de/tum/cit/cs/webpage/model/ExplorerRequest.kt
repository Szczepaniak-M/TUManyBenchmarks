package de.tum.cit.cs.webpage.model

data class ExplorerRequest(
    val partialResults: Boolean,
    val aggregationStages: List<String>
)