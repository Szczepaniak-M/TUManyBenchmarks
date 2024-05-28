package de.tum.cit.cs.benchmarkservice.service

import com.cronutils.model.definition.CronDefinition
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import de.tum.cit.cs.benchmarkservice.model.BenchmarkCron
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class CronParserService {

    private val cronDefinition: CronDefinition =
        CronDefinitionBuilder.defineCron()
            .withMinutes().and()
            .withHours().and()
            .withDayOfMonth().supportsHash().supportsL().supportsW().and()
            .withMonth().and()
            .withDayOfWeek().supportsHash().supportsL().supportsW().and()
            .instance()
    private val parser = CronParser(cronDefinition)

    fun isCronActive(benchmarkCron: BenchmarkCron, currentTime: ZonedDateTime): Boolean {
        val cron = parser.parse(benchmarkCron.cron)
        val executionTime = ExecutionTime.forCron(cron)
        return executionTime.isMatch(currentTime)
    }
}
