package com.finovara.activityservice.security;

import com.finovara.activityservice.security.jwt.JwtAuthenticationFilter;
import com.finovara.activityservice.security.jwt.JwtService;
import com.finovara.activityservice.security.jwt.JwtTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityFilterChainConfigTest.TestController.class)
@Import({SecurityFilterChainConfig.class, JwtAuthenticationFilter.class})
class SecurityFilterChainConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @Test
    void shouldReturnUnauthorizedForEndpointWithoutJwt() throws Exception {
        when(jwtTokenResolver.resolve(any(HttpServletRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/security-test"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class TestController {

        @GetMapping("/security-test")
        String securedEndpoint() {
            return "ok";
        }
    }
}
