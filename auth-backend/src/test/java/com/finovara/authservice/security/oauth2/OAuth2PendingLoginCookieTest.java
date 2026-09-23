package com.finovara.authservice.security.oauth2;

import com.finovara.authservice.security.oauth2.OAuth2PendingLoginCookie.PendingLogin;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2PendingLoginCookieTest {

    private static final String SECRET = "test-secret-1234567890";
    private static final Long USER_ID = 1L;
    private static final String SOURCE_EVENT_ID = "source-event-id";
    private static final long TTL_SECONDS = 600L;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    private OAuth2PendingLoginCookie pendingLoginCookie;

    @BeforeEach
    void setUp() {
        pendingLoginCookie = new OAuth2PendingLoginCookie(SECRET, TTL_SECONDS);
    }

    private Cookie captureAddedCookie(boolean secure) {
        pendingLoginCookie.add(response, USER_ID, SOURCE_EVENT_ID, secure);

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        return captor.getValue();
    }

    @Nested
    class Add {

        @Test
        void shouldAddCookieWithCorrectNameWhenAdding() {
            Cookie cookie = captureAddedCookie(true);

            assertThat(cookie.getName()).isEqualTo(OAuth2PendingLoginCookie.COOKIE_NAME);
        }

        @Test
        void shouldAddHttpOnlyCookieWhenAdding() {
            Cookie cookie = captureAddedCookie(true);

            assertThat(cookie.isHttpOnly()).isTrue();
        }

        @Test
        void shouldAddSecureCookieWhenSecureTrue() {
            Cookie cookie = captureAddedCookie(true);

            assertThat(cookie.getSecure()).isTrue();
        }

        @Test
        void shouldAddNonSecureCookieWhenSecureFalse() {
            Cookie cookie = captureAddedCookie(false);

            assertThat(cookie.getSecure()).isFalse();
        }

        @Test
        void shouldSetCookiePathToRootWhenAdding() {
            Cookie cookie = captureAddedCookie(true);

            assertThat(cookie.getPath()).isEqualTo("/");
        }

        @Test
        void shouldSetCookieMaxAgeToTtlSecondsWhenAdding() {
            Cookie cookie = captureAddedCookie(true);

            assertThat(cookie.getMaxAge()).isEqualTo((int) TTL_SECONDS);
        }

        @Test
        void shouldEncryptCookieValueWhenAdding() {
            Cookie cookie = captureAddedCookie(true);

            assertThat(cookie.getValue()).doesNotContain(USER_ID.toString() + ":" + SOURCE_EVENT_ID);
        }
    }

    @Nested
    class Read {

        @Test
        void shouldReturnEmptyWhenCookieArrayIsNull() {
            when(request.getCookies()).thenReturn(null);

            Optional<PendingLogin> result = pendingLoginCookie.read(request);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNoCookiesPresent() {
            when(request.getCookies()).thenReturn(new Cookie[0]);

            Optional<PendingLogin> result = pendingLoginCookie.read(request);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenPendingLoginCookieNotFound() {
            Cookie otherCookie = new Cookie("other_cookie", "value");
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

            Optional<PendingLogin> result = pendingLoginCookie.read(request);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnPendingLoginWhenValidCookiePresent() {
            Cookie addedCookie = captureAddedCookie(true);
            when(request.getCookies()).thenReturn(new Cookie[]{addedCookie});

            Optional<PendingLogin> result = pendingLoginCookie.read(request);

            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(USER_ID);
            assertThat(result.get().sourceEventId()).isEqualTo(SOURCE_EVENT_ID);
        }

        @Test
        void shouldReturnPendingLoginWhenMultipleCookiesPresentAndOursIsAmongThem() {
            Cookie addedCookie = captureAddedCookie(true);
            Cookie otherCookie = new Cookie("session_id", "abc123");
            when(request.getCookies()).thenReturn(new Cookie[]{otherCookie, addedCookie});

            Optional<PendingLogin> result = pendingLoginCookie.read(request);

            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(USER_ID);
        }

        @Test
        void shouldReturnEmptyWhenCookieValueIsCorrupted() {
            Cookie corruptedCookie = new Cookie(OAuth2PendingLoginCookie.COOKIE_NAME, "garbage-value");
            when(request.getCookies()).thenReturn(new Cookie[]{corruptedCookie});

            Optional<PendingLogin> result = pendingLoginCookie.read(request);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenCookieHasExpired() {
            OAuth2PendingLoginCookie expiredIssuer = new OAuth2PendingLoginCookie(SECRET, -10L);
            expiredIssuer.add(response, USER_ID, SOURCE_EVENT_ID, true);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            Cookie expiredCookie = captor.getValue();

            when(request.getCookies()).thenReturn(new Cookie[]{expiredCookie});

            Optional<PendingLogin> result = pendingLoginCookie.read(request);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Clear {

        @Test
        void shouldAddTwoCookiesWhenClearing() {
            pendingLoginCookie.clear(response);

            verify(response, times(2)).addCookie(any(Cookie.class));
        }

        @Test
        void shouldAddOneSecureAndOneNonSecureCookieWhenClearing() {
            pendingLoginCookie.clear(response);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response, times(2)).addCookie(captor.capture());
            List<Cookie> cookies = captor.getAllValues();

            assertThat(cookies).extracting(Cookie::getSecure).containsExactlyInAnyOrder(true, false);
        }

        @Test
        void shouldSetEmptyValueAndZeroMaxAgeWhenClearing() {
            pendingLoginCookie.clear(response);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response, times(2)).addCookie(captor.capture());

            assertThat(captor.getAllValues())
                    .allSatisfy(cookie -> {
                        assertThat(cookie.getValue()).isEmpty();
                        assertThat(cookie.getMaxAge()).isZero();
                        assertThat(cookie.getName()).isEqualTo(OAuth2PendingLoginCookie.COOKIE_NAME);
                    });
        }
    }
}