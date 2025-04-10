package de.tum.cit.cs.webpage.repository

import de.tum.cit.cs.webpage.MongoTestContainerConfig
import de.tum.cit.cs.webpage.model.Instance
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal

@ActiveProfiles("test")
@DataMongoTest
@Testcontainers
@EnableReactiveMongoRepositories
@ContextConfiguration(classes = [MongoTestContainerConfig::class])
class InstanceRepositoryTests {

    @Autowired
    lateinit var instanceRepository: InstanceRepository


    @Test
    fun `find instance details by instance name`() = runTest {
        // given
        val instance1 = Instance("id1", "t2.micro", BigDecimal.ZERO, BigDecimal.ZERO, 1, BigDecimal.ONE, "Low", "EBS only", emptyList(), emptyList())
        val instance2 = Instance("id2", "t2.small", BigDecimal.ZERO, BigDecimal.ZERO, 2, BigDecimal.TWO, "Low", "EBS only", emptyList(), emptyList())
        instanceRepository.save(instance1)
        instanceRepository.save(instance2)

        // when
        val result = instanceRepository.findByName("t2.micro")

        // then
        assertNotNull(result)
        assertEquals(instance1, result)
    }

    @Test
    fun `not find instance details by instance name when it does not exists`() = runTest {
        // given
        val instance1 = Instance("id1", "t2.micro", BigDecimal.ZERO, BigDecimal.ZERO, 1, BigDecimal.ONE, "Low", "EBS only", emptyList(), emptyList())
        instanceRepository.save(instance1)

        // when
        val result = instanceRepository.findByName("nonExistingName")

        // then
        assertNull(result)
    }
}
