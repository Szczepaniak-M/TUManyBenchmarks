package de.tum.cit.cs.webpage

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebpageBackendApplication

fun main(args: Array<String>) {
	runApplication<WebpageBackendApplication>(*args)
}
