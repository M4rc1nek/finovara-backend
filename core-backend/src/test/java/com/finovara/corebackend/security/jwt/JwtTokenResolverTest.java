package com.finovara.corebackend.security.jwt;

import com.finovara.corebackend.security.oauth2.OAuth2AccessTokenCookie;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenResolverTest {

    private JwtTokenResolver jwtTokenResolver;

    @BeforeEach
    void setUp() {
        jwtTokenResolver = new JwtTokenResolver();
    }

    @Nested
    class AuthorizationHeaderTests {

        @Test
        void shouldResolveBearerToken() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer jwt-token");

            Optional<String> token = jwtTokenResolver.resolve(request);

            assertThat(token).contains("jwt-token");
        }

        @Test
        void shouldIgnoreAuthorizationHeaderWithoutBearerPrefix() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "jwt-token");

            Optional<String> token = jwtTokenResolver.resolve(request);

            assertThat(token).isEmpty();
        }
    }

    @Nested
    class CookieTests {

        @Test
        void shouldResolveOAuth2AccessTokenCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie(OAuth2AccessTokenCookie.COOKIE_NAME, "cookie-token"));

            Optional<String> token = jwtTokenResolver.resolve(request);

            assertThat(token).contains("cookie-token");
        }

        @Test
        void shouldPreferBearerTokenOverCookie() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer bearer-token");
            request.setCookies(new Cookie(OAuth2AccessTokenCookie.COOKIE_NAME, "cookie-token"));

            Optional<String> token = jwtTokenResolver.resolve(request);

            assertThat(token).contains("bearer-token");
        }
    }
}
