package com.finovara.corebackend.security.oauth2;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2AccessTokenCookieTest {

    private MockHttpServletResponse mockHttpServletResponse;

    @BeforeEach
    void setUp() {
        mockHttpServletResponse = new MockHttpServletResponse();
    }

    @Nested
    class AddCookieTests {

        @Test
        void shouldSetCookieNameProperly() {
            OAuth2AccessTokenCookie.add(mockHttpServletResponse, "abc", true);

            Cookie cookie = mockHttpServletResponse.getCookie(OAuth2AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getName()).isEqualTo(OAuth2AccessTokenCookie.COOKIE_NAME);
        }

        @Test
        void shouldSetPathToRoot() {
            OAuth2AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(OAuth2AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getPath()).isEqualTo("/");
        }

        @Test
        void shouldSetSecureCookie() {
            OAuth2AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(OAuth2AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getSecure()).isTrue();
        }

        @Test
        void shouldSetNonSecureCookie() {
            OAuth2AccessTokenCookie.add(mockHttpServletResponse, "token123", false);

            Cookie cookie = mockHttpServletResponse.getCookie(OAuth2AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getSecure()).isFalse();
        }

        @Test
        void shouldSetHttpOnlyFlag() {
            OAuth2AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(OAuth2AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.isHttpOnly()).isTrue();
        }

        @Test
        void shouldSetCorrectMaxAge() {
            OAuth2AccessTokenCookie.add(mockHttpServletResponse, "token123", true);

            Cookie cookie = mockHttpServletResponse.getCookie(OAuth2AccessTokenCookie.COOKIE_NAME);

            assertThat(cookie).isNotNull();
            assertThat(cookie.getMaxAge()).isEqualTo(24 * 60 * 60);
        }
    }

    @Nested
    class ClearCookieTests {

        @BeforeEach
        void clearCookie() {
            OAuth2AccessTokenCookie.clear(mockHttpServletResponse);
        }

        @Test
        void shouldClearCookieValue() {
            Cookie[] cookies = mockHttpServletResponse.getCookies();

            assertThat(cookies).extracting(Cookie::getValue)
                    .containsOnly("");
        }

        @Test
        void shouldCreateTwoSetCookieHeadersWhenClearing() {
            assertThat(mockHttpServletResponse.getHeaders("Set-Cookie")).hasSize(2);
        }

        @Test
        void shouldClearBothSecureAndNonSecureCookies() {
            Cookie[] cookies = mockHttpServletResponse.getCookies();

            assertThat(cookies).extracting(Cookie::getSecure)
                    .containsExactlyInAnyOrder(true, false);
        }

        @Test
        void shouldSetMaxAgeToZeroWhenClearing() {
            Cookie[] cookies = mockHttpServletResponse.getCookies();

            assertThat(cookies).extracting(Cookie::getMaxAge)
                    .containsOnly(0);
        }
    }
}