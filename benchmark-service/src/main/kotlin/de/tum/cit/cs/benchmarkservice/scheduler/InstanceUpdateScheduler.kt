package de.tum.cit.cs.benchmarkservice.scheduler

import de.tum.cit.cs.benchmarkservice.service.InstanceService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!test")
@EnableScheduling
class InstanceUpdateScheduler(
    private val instanceService: InstanceService
) {

    private val logger = KotlinLogging.logger {}

    // check instances on start and every Sunday at 00:30
    @Scheduled(cron = "0 30 0 * * SUN")
    @EventListener(ApplicationReadyEvent::class)
    fun updateInstances() {
        logger.info { "Starting updating AWS EC2 instances information" }
        instanceService.updateInstances()
        logger.info { "Finished updating AWS EC2 instances information" }
    }

}
