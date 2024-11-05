package de.tum.cit.cs.benchmarkservice.scheduler

import de.tum.cit.cs.benchmarkservice.service.BenchmarkService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
@Profile("!test")
@EnableScheduling
class BenchmarkRunnerScheduler(
    private val benchmarkService: BenchmarkService
) {

    private val logger = KotlinLogging.logger {}

//    @EventListener(ApplicationReadyEvent::class) // for test only
    // check benchmarks to run every hour
    @Scheduled(cron = "0 0 * * * *")
    fun runBenchmarks() {
        if(benchmarkService.isBenchmarkExecutionAllowed()){
            val time = ZonedDateTime.now().withSecond(0).withNano(0).toString()
            logger.info { "Staring benchmarks scheduled on $time" }
            benchmarkService.runBenchmarks()
            logger.info { "Finished benchmarks scheduled on $time" }
        }
    }
}