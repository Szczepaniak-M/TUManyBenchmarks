package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.BenchmarkCron
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class CronParserServiceTest {

    private val TEST_ID = "ID"
    private val DEFUALT_DATE: ZonedDateTime = ZonedDateTime.now().withMinute(0)
    private val cronParserService = CronParserService()

    @Test
    fun testMatchingSimpleCronExpression() {
        // given
        val benchmarkCron = BenchmarkCron(TEST_ID, "0 * * * *") // run every hour
        // when
        val isActive = cronParserService.isCronActive(benchmarkCron, DEFUALT_DATE)
        // then
        assertTrue(isActive)
    }

    @Test
    fun testMatchingComplexCronExpression() {
        // given
        val benchmarkCron = BenchmarkCron(TEST_ID, "0 0 L * *") // run last day of month
        val date = DEFUALT_DATE.withHour(0).withDayOfMonth(31).withMonth(1)
        // when
        val isActive = cronParserService.isCronActive(benchmarkCron, date)
        // then
        assertTrue(isActive)
    }

    @Test
    fun testNonMatchingCronExpression() {
        // given
        val benchmarkCron = BenchmarkCron(TEST_ID, "0 0 L * *") // run last day of month
        val date = DEFUALT_DATE.withHour(0).withDayOfMonth(30).withMonth(1)
        // when
        val isActive = cronParserService.isCronActive(benchmarkCron, date)
        // then
        assertFalse(isActive)
    }

}