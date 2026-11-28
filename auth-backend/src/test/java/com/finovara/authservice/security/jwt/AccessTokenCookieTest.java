package com.finovara.authservice.security.jwt;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenCookieTest {

    private MockHttpServletResponse mockHttpServletResponse;

    @BeforeEach
    void setUp() {
        mockHttpServletResponse = new MockHttpServletResponse();
    }

    @Nested
    class AddCookieTests {

        @Test
        void shouldPreserveExistingAccessTokenCookieName() {
            AccessTokenCookie.add(mockHttpServletResponse, "abc", true);

            Cookie cookie = mockHttpServletResponse.getCookie(AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getName()).isEqualTo("oauth2_access_token");
        }

        @Test
        void shouldSetPathToRoot() {
            AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getPath()).isEqualTo("/");
        }

        @Test
        void shouldSetSecureCookie() {
            AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getSecure()).isTrue();
        }

        @Test
        void shouldSetNonSecureCookie() {
            AccessTokenCookie.add(mockHttpServletResponse, "token123", false);

            Cookie cookie = mockHttpServletResponse.getCookie(AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getSecure()).isFalse();
        }

        @Test
        void shouldSetHttpOnlyFlag() {
            AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.isHttpOnly()).isTrue();
        }

        @Test
        void shouldSetCorrectMaxAge() {
            AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getMaxAge()).isEqualTo(24 * 60 * 60);
        }
    }

    @Nested
    class ClearCookieTests {

        @BeforeEach
        void clearCookie() {
            AccessTokenCookie.clear(mockHttpServletResponse);
        }

        @Test
        void shouldClearCookieValue() {
            Cookie[] cookies = mockHttpServletResponse.getCookies();

            assertThat(cookies).extracting(Cookie::getValue)
                    .containsOnly("");
        }

        @Test
        void shouldClearCurrentAndLegacyCookieNames() {
            assertThat(mockHttpServletResponse.getHeaders("Set-Cookie")).hasSize(4);
            assertThat(mockHttpServletResponse.getCookies())
                    .extracting(Cookie::getName)
                    .containsExactlyInAnyOrder(
                            AccessTokenCookie.COOKIE_NAME,
                            AccessTokenCookie.COOKIE_NAME,
                            AccessTokenCookie.ALTERNATE_COOKIE_NAME,
                            AccessTokenCookie.ALTERNATE_COOKIE_NAME);
        }

        @Test
        void shouldClearBothSecureAndNonSecureCookies() {
            Cookie[] cookies = mockHttpServletResponse.getCookies();

            assertThat(cookies).extracting(Cookie::getSecure)
                    .containsExactlyInAnyOrder(true, true, false, false);
        }

        @Test
        void shouldSetMaxAgeToZeroWhenClearing() {
            Cookie[] cookies = mockHttpServletResponse.getCookies();

            assertThat(cookies).extracting(Cookie::getMaxAge)
                    .containsOnly(0);
        }
    }
}