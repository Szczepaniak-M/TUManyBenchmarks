package de.tum.cit.cs.webpage.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.Instance
import de.tum.cit.cs.webpage.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import kotlin.test.assertNull

@ExtendWith(SpringExtension::class)
class InstanceServiceTest {

    private lateinit var service: InstanceService

    @MockkBean
    private lateinit var instanceRepository: InstanceRepository

    companion object {
        private val INSTANCE_1 = Instance("id1", "t2.micro", 1, BigDecimal(1), "Low", emptyList(), emptyList())
        private val INSTANCE_2 = Instance("id2", "t3.micro", 2, BigDecimal(2), "Low", emptyList(), emptyList())
    }


    @BeforeEach
    fun setUp() {
        service = InstanceService(instanceRepository)
    }

    @Test
    fun `find all instances and cache them`() = runTest {
        // given
        coEvery { instanceRepository.findAll() } returns flowOf(INSTANCE_1, INSTANCE_2)

        // when
        val databaseResult = service.findAll(null, null).toList()
        val cachedResult = service.findAll(null, null).toList()

        // then
        coVerify(exactly = 1) { instanceRepository.findAll() }
        assertThat(databaseResult).hasSameElementsAs(listOf(INSTANCE_1, INSTANCE_2))
        assertThat(cachedResult).hasSameElementsAs(listOf(INSTANCE_1, INSTANCE_2))
    }

    @Test
    fun `find instance details by instance type and cache result`() = runTest {
        // given
        val instanceType = "t3.micro"
        coEvery { instanceRepository.findByName(instanceType) } returns INSTANCE_1

        // when
        val databaseResult = service.findByInstanceType(instanceType, null, null)
        val cachedResult = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 1) { instanceRepository.findByName(instanceType) }
        assertNotNull(databaseResult)
        assertEquals(INSTANCE_1, databaseResult)
        assertNotNull(cachedResult)
        assertEquals(INSTANCE_1, databaseResult)
    }

    @Test
    fun `do not cache result if there is no instance details`() = runTest {
        // given
        val instanceType = "nonExisting"
        coEvery { instanceRepository.findByName(instanceType) } returns null

        // when
        val databaseResult1 = service.findByInstanceType(instanceType, null, null)
        val databaseResult2 = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 2) { instanceRepository.findByName(instanceType) }
        assertNull(databaseResult1)
        assertNull(databaseResult2)
    }
}
