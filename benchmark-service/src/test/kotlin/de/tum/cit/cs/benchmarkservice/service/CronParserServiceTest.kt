package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.BenchmarkCron
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class CronParserServiceTest {

    companion object {
        private const val TEST_ID = "ID"
        private val DEFAULT_DATE = ZonedDateTime.now().withMinute(0)
        private val cronParserService = CronParserService()
    }

    @Test
    fun `test matching simple cron expression`() {
        // given
        val benchmarkCron = BenchmarkCron(TEST_ID, "0 * * * *") // run every hour
        // when
        val isActive = cronParserService.isCronActive(benchmarkCron, DEFAULT_DATE)
        // then
        assertTrue(isActive)
    }

    @Test
    fun `test matching complex cron expression`() {
        // given
        val benchmarkCron = BenchmarkCron(TEST_ID, "0 0 L * *") // run last day of month
        val date = DEFAULT_DATE.withHour(0).withDayOfMonth(31).withMonth(1)
        // when
        val isActive = cronParserService.isCronActive(benchmarkCron, date)
        // then
        assertTrue(isActive)
    }

    @Test
    fun `test non-matching cron expression`() {
        // given
        val benchmarkCron = BenchmarkCron(TEST_ID, "0 0 L * *") // run last day of month
        val date = DEFAULT_DATE.withHour(0).withDayOfMonth(30).withMonth(1)
        // when
        val isActive = cronParserService.isCronActive(benchmarkCron, date)
        // then
        assertFalse(isActive)
    }
}
