package com.clearspend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Loads full context → needs a live MongoDB; skip in CI. Add Testcontainers later for an integration profile.")
class ClearspendApplicationTests {

	@Test
	void contextLoads() {
	}

}
