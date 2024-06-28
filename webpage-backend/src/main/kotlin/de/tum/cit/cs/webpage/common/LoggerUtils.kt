package de.tum.cit.cs.webpage.common

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


object LoggerUtils {

    fun buildDebugLogMessage(
        message: String,
        requestId: String?,
        apiKey: String?,
    ): String = buildLogMessage("DEBUG", message, requestId, apiKey)

    fun buildInfoLogMessage(
        message: String,
        requestId: String?,
        apiKey: String?,
    ): String = buildLogMessage("INFO", message, requestId, apiKey)

    private fun buildLogMessage(
        level: String,
        message: String,
        requestId: String?,
        apiKey: String?,
    ): String {
        val timestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())

        val formattedMessage = if (message.startsWith("{")) {
            message
        } else {
            "\"$message\""
        }
        return "{ \"timestamp\": \"$timestamp\", " +
                "\"level\": \"$level\", " +
                "\"message\": $formattedMessage, " +
                "\"requestId\": \"$requestId\", " +
                "\"apiKey\": \"$apiKey\" }"

    }
}
