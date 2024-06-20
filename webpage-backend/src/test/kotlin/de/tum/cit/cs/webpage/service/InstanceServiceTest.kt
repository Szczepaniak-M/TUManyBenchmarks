package de.tum.cit.cs.webpage.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.Instance
import de.tum.cit.cs.webpage.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class InstanceServiceTest {

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    companion object {
        private val INSTANCE_1 = Instance("id1", "t2.micro", emptyList())
        private val INSTANCE_2 = Instance("id2", "t3.micro", emptyList())
    }

    @Test
    fun `find all instances and cache them`() = runTest {
        val service = InstanceService(instanceRepository)
        coEvery { instanceRepository.findAll() } returns flowOf(INSTANCE_1, INSTANCE_2)

        // when
        val databaseResult = service.findAll().toList()
        val cachedResult = service.findAll().toList()

        // then
        coVerify(exactly = 1) { instanceRepository.findAll() }
        assertEquals(listOf(INSTANCE_1, INSTANCE_2), databaseResult)
        assertEquals(listOf(INSTANCE_1, INSTANCE_2), cachedResult)
    }
}
