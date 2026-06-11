package com.finovara.activitylogservice.security.jwt;

import jakarta.servlet.http.Cookie;
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

    @Test
    void shouldResolveOAuth2AccessTokenFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("oauth2_access_token", "oauth2-token-123"));

        assertThat(jwtTokenResolver.resolve(request)).contains("oauth2-token-123");
    }

    @Test
    void shouldPreferBearerTokenOverOAuth2Cookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bearer-token-123");
        request.setCookies(new Cookie("oauth2_access_token", "oauth2-token-123"));

        assertThat(jwtTokenResolver.resolve(request)).contains("bearer-token-123");
    }
}
