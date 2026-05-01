package com.soccialy.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestPropertySource("classpath:application.properties") // Nu gaseste Spring calea catrea test/resources/application.properties. Încer asta.
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
