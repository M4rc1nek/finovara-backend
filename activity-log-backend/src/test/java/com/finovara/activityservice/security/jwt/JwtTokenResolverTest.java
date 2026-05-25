package com.finovara.activityservice.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenResolverTest {

    private final JwtTokenResolver jwtTokenResolver = new JwtTokenResolver();

    @Test
    void shouldResolveBearerTokenFromAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-123");

        assertThat(jwtTokenResolver.resolve(request)).contains("token-123");
    }

    @Test
    void shouldReturnEmptyWhenAuthorizationHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(jwtTokenResolver.resolve(request)).isEmpty();
    }
}
