package com.finovara_backend.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"MONOLIT_URL=https://localhost:8443",
		"SSL_KEY_STORE_PASSWORD=test"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
