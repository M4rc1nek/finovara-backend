package com.finovara.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "CORE_BACKEND_URL=https://localhost:8443",
        "ACTIVITY_LOG_URL=https://finovara-activity:8082",
        "SSL_KEY_STORE_PASSWORD=test"
})
class ApiGatewayApplicationTests {
    @Test
    void contextLoads() {
    }
}
