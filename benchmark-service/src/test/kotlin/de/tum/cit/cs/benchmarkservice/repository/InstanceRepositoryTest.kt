package de.tum.cit.cs.benchmarkservice.repository

import de.tum.cit.cs.benchmarkservice.MongoTestContainerConfig
import de.tum.cit.cs.benchmarkservice.model.Instance
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

@ActiveProfiles("test")
@DataMongoTest
@Testcontainers
@EnableReactiveMongoRepositories
@ContextConfiguration(classes = [MongoTestContainerConfig::class])
class InstanceRepositoryTest {

    @Autowired
    lateinit var instanceRepository: InstanceRepository

    companion object {
        private const val ID_1 = "id1"
        private const val ID_2 = "id2"
        private const val ID_3 = "id3"
        private const val NAME_1 = "t3.micro"
        private const val NAME_2 = "c7gd.metal"
        private val TAGS_1 = listOf("2 vCPU", "1.0 GiB")
        private val TAGS_2 = listOf("64 vCPU", "128 GiB")
        private val TAGS_3 = listOf("64 vCPU", "128 GiB", "SSD")
    }

    @Test
    fun `update tags by Id when matching instance exists`() = runTest {
        // given
        val instance1 = Instance(ID_1, NAME_1, TAGS_1)
        val instance2 = Instance(ID_2, NAME_2, TAGS_2)
        instanceRepository.save(instance1)
        instanceRepository.save(instance2)

        // when
        val updateCount = instanceRepository.updateTagsById(ID_1, TAGS_3)

        // then
        assertEquals(1, updateCount)
        val updatedInstance = instanceRepository.findById(ID_1)
        assertNotNull(updatedInstance)
        assertEquals(NAME_1, updatedInstance?.name)
        assertEquals(TAGS_3, updatedInstance?.tags)
        val notUpdatedInstance = instanceRepository.findById(ID_2)
        assertNotNull(notUpdatedInstance)
        assertEquals(NAME_2, notUpdatedInstance?.name)
        assertEquals(TAGS_2, notUpdatedInstance?.tags)
    }

    @Test
    fun `update tags by Id when matching instance not exists`() = runTest {
        // given
        val instance1 = Instance(ID_1, NAME_1, TAGS_1)
        val instance2 = Instance(ID_2, NAME_2, TAGS_2)
        instanceRepository.save(instance1)
        instanceRepository.save(instance2)

        // when
        val updateCount = instanceRepository.updateTagsById(ID_3, TAGS_3)

        // then
        assertEquals(0, updateCount)
        val updatedInstance = instanceRepository.findById(ID_1)
        assertNotNull(updatedInstance)
        assertEquals(NAME_1, updatedInstance?.name)
        assertEquals(TAGS_1, updatedInstance?.tags)
        val notUpdatedInstance = instanceRepository.findById(ID_2)
        assertNotNull(notUpdatedInstance)
        assertEquals(NAME_2, notUpdatedInstance?.name)
        assertEquals(TAGS_2, notUpdatedInstance?.tags)
    }
}
