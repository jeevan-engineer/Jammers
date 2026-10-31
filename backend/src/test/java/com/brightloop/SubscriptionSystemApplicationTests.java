package com.brightloop;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires live MySQL instance configured in application.properties")
class SubscriptionSystemApplicationTests {

	@Test
	void contextLoads() {
	}
}
