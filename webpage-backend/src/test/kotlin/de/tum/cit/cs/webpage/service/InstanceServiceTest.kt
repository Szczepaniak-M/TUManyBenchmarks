package de.tum.cit.cs.webpage.service

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.model.Instance
import de.tum.cit.cs.webpage.repository.InstanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
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

    @MockkBean
    private lateinit var ec2PriceService: Ec2PriceService

    companion object {
        private val INSTANCE_1 = Instance("id1", "t2.micro", BigDecimal.ZERO, BigDecimal.ZERO, 1, BigDecimal.ONE, "Low", "EBS only", emptyList(), emptyList())
        private val INSTANCE_2 = Instance("id2", "t3.micro", BigDecimal.ZERO, BigDecimal.ZERO, 2, BigDecimal.TWO, "Low", "EBS only", emptyList(), emptyList())
        private val INSTANCE_1_RESULT = INSTANCE_1.copy(onDemandPrice = BigDecimal.ONE, spotPrice = BigDecimal.TWO)
        private val INSTANCE_2_RESULT = INSTANCE_2.copy(onDemandPrice = BigDecimal.TWO, spotPrice = BigDecimal.TEN)
    }

    @BeforeEach
    fun setUp() {
        service = InstanceService(instanceRepository, ec2PriceService)
    }

    @Test
    fun `find all instances and cache them`() = runTest {
        // given
        coEvery { instanceRepository.findAll() } returns flowOf(INSTANCE_1, INSTANCE_2)
        every { ec2PriceService.getOnDemandPrice("t2.micro") } returns BigDecimal.ONE
        every { ec2PriceService.getOnDemandPrice("t3.micro") } returns BigDecimal.TWO
        every { ec2PriceService.getSpotPrice("t2.micro") } returns BigDecimal.TWO
        every { ec2PriceService.getSpotPrice("t3.micro") } returns BigDecimal.TEN

        // when
        val databaseResult = service.findAll(null, null).toList()
        val cachedResult = service.findAll(null, null).toList()

        // then
        coVerify(exactly = 1) { instanceRepository.findAll() }
        coVerify(exactly = 1) { ec2PriceService.getOnDemandPrice("t2.micro") }
        coVerify(exactly = 1) { ec2PriceService.getOnDemandPrice("t3.micro") }
        coVerify(exactly = 1) { ec2PriceService.getSpotPrice("t2.micro") }
        coVerify(exactly = 1) { ec2PriceService.getSpotPrice("t3.micro") }
        assertThat(databaseResult).hasSameElementsAs(listOf(INSTANCE_1_RESULT, INSTANCE_2_RESULT))
        assertThat(cachedResult).hasSameElementsAs(listOf(INSTANCE_1_RESULT, INSTANCE_2_RESULT))
    }

    @Test
    fun `find instance details by instance type when no cache`() = runTest {
        // given
        val instanceType = "t2.micro"
        coEvery { instanceRepository.findByName(instanceType) } returns INSTANCE_1
        every { ec2PriceService.getOnDemandPrice("t2.micro") } returns BigDecimal.ONE
        every { ec2PriceService.getSpotPrice("t2.micro") } returns BigDecimal.TWO

        // when
        val cachedResult = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 1) { instanceRepository.findByName(instanceType) }
        coVerify(exactly = 1) { ec2PriceService.getOnDemandPrice("t2.micro") }
        coVerify(exactly = 1) { ec2PriceService.getSpotPrice("t2.micro") }
        assertEquals(INSTANCE_1_RESULT, cachedResult)
    }

    @Test
    fun `find instance details by instance type using cache result`() = runTest {
        // given
        val instanceType = "t2.micro"
        coEvery { instanceRepository.findAll() } returns flowOf(INSTANCE_1, INSTANCE_2)
        every { ec2PriceService.getOnDemandPrice("t2.micro") } returns BigDecimal.ONE
        every { ec2PriceService.getOnDemandPrice("t3.micro") } returns BigDecimal.TWO
        every { ec2PriceService.getSpotPrice("t2.micro") } returns BigDecimal.TWO
        every { ec2PriceService.getSpotPrice("t3.micro") } returns BigDecimal.TEN

        // when
        service.findAll(null, null).toList()
        val cachedResult = service.findByInstanceType(instanceType, null, null)

        // then
        coVerify(exactly = 0) { instanceRepository.findByName(instanceType) }
        coVerify(exactly = 1) { ec2PriceService.getOnDemandPrice("t2.micro") }
        coVerify(exactly = 1) { ec2PriceService.getOnDemandPrice("t3.micro") }
        coVerify(exactly = 1) { ec2PriceService.getSpotPrice("t2.micro") }
        coVerify(exactly = 1) { ec2PriceService.getSpotPrice("t3.micro") }
        assertEquals(INSTANCE_1_RESULT, cachedResult)
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
