package de.tum.cit.cs.benchmarkservice.service

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow


@Service
class GitHubService(
    private val gitHubClient: WebClient
) {

    suspend fun getCurlsForFilesFromDirectory(baseDirectory: String): List<String> {
        val curlsCommands = mutableListOf<String>()
        val directories = mutableListOf<String>()
        gitHubClient.get()
            .uri(baseDirectory)
            .retrieve()
            .bodyToFlow<GitHubFile>()
            .collect {
                if (it.type == "dir") {
                    directories.add(it.path)
                } else if (it.type == "file" && it.downloadUrl != null) {
                    curlsCommands.add("curl --create-dirs -o \"${it.path}\" \"${it.downloadUrl}\"")
                }
            }
        val subdirectories = directories.flatMap { getCurlsForFilesFromDirectory(it) }
        curlsCommands.addAll(subdirectories)
        return curlsCommands
    }

    data class GitHubFile(
        val type: String,
        val path: String,
        @JsonProperty("download_url") val downloadUrl: String?
    )
}

