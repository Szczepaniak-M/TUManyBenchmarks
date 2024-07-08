package de.tum.cit.cs.benchmarkservice.scheduler

import de.tum.cit.cs.benchmarkservice.service.BenchmarkService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component

@Component
@Profile("!test")
@EnableScheduling
class BenchmarkRunnerScheduler(
    private val benchmarkService: BenchmarkService
) {

    private val logger = KotlinLogging.logger {}

    // check benchmarks to run every hour
//    @Scheduled(cron = "0 0 * * * *")
    @EventListener(ApplicationReadyEvent::class) // for test only
    fun runBenchmarks() {
        logger.info { "Staring benchmarks" }
        benchmarkService.runBenchmarks()
        logger.info { "Finished benchmarks" }
    }
}