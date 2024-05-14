package de.tum.cit.cs.benchmarkservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BenchmarkServiceApplication

fun main(args: Array<String>) {
	runApplication<BenchmarkServiceApplication>(*args)
}
