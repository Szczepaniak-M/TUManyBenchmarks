package de.tum.cit.cs.webpage

import com.ninjasquad.springmockk.MockkBean
import de.tum.cit.cs.webpage.service.Ec2PriceService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class WebpageBackendApplicationTests {

	@MockkBean
	private lateinit var ec2PriceService: Ec2PriceService

	@Test
	fun contextLoads() {
	}

}
