package com.finovara.authbackend.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthorizationRequestCookieStoreTest {

    @InjectMocks
    private OAuth2AuthorizationRequestCookieStore store;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    private OAuth2AuthorizationRequest oAuth2AuthorizationRequest;

    @BeforeEach
    void setUp() {
        oAuth2AuthorizationRequest = mock(OAuth2AuthorizationRequest.class);
    }

    @Nested
    class LoadAuthorizationRequest {

        @Test
        void shouldReturnNullWhenCookiesArrayIsNull() {
            when(httpServletRequest.getCookies()).thenReturn(null);

            OAuth2AuthorizationRequest result = store.loadAuthorizationRequest(httpServletRequest);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenCookieArrayIsEmpty() {
            when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{});

            OAuth2AuthorizationRequest result = store.loadAuthorizationRequest(httpServletRequest);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenCookieNameDoesNotMatch() {
            Cookie cookie = new Cookie("wrong_cookie", "value");
            when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

            OAuth2AuthorizationRequest result = store.loadAuthorizationRequest(httpServletRequest);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenCookieValueIsCorrupted() {
            Cookie cookie = new Cookie("oauth2_auth_request", "invalid_base64");
            when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

            OAuth2AuthorizationRequest result = store.loadAuthorizationRequest(httpServletRequest);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenCookieValueIsEmpty() {
            Cookie cookie = new Cookie("oauth2_auth_request", "");
            when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

            OAuth2AuthorizationRequest result = store.loadAuthorizationRequest(httpServletRequest);

            assertNull(result);
        }
    }

    @Nested
    class SaveAuthorizationRequest {

        @Test
        void shouldRemoveCookiesWhenAuthorizationRequestIsNull() {
            store.saveAuthorizationRequest(null, httpServletRequest, httpServletResponse);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(httpServletResponse, times(2)).addCookie(captor.capture());

            assertEquals(2, captor.getAllValues().size());
            assertTrue(captor.getAllValues().stream().anyMatch(Cookie::getSecure));
            assertTrue(captor.getAllValues().stream().anyMatch(cookie -> !cookie.getSecure()));
            captor.getAllValues().forEach(cookie -> {
                assertEquals("oauth2_auth_request", cookie.getName());
                assertEquals("", cookie.getValue());
                assertEquals(0, cookie.getMaxAge());
                assertEquals("/", cookie.getPath());
                assertTrue(cookie.isHttpOnly());
            });
        }

        @Test
        void shouldCreateCookieWithSecureFlagWhenRequestIsSecure() {
            when(httpServletRequest.isSecure()).thenReturn(true);

            store.saveAuthorizationRequest(mock(OAuth2AuthorizationRequest.class), httpServletRequest, httpServletResponse);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(httpServletResponse).addCookie(captor.capture());

            Cookie cookie = captor.getValue();
            assertTrue(cookie.getSecure());
            assertEquals("/", cookie.getPath());
            assertTrue(cookie.isHttpOnly());
        }

        @Test
        void shouldCreateCookieWithNonSecureFlagWhenRequestIsNotSecure() {
            when(httpServletRequest.isSecure()).thenReturn(false);

            store.saveAuthorizationRequest(mock(OAuth2AuthorizationRequest.class), httpServletRequest, httpServletResponse);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(httpServletResponse).addCookie(captor.capture());

            Cookie cookie = captor.getValue();
            assertFalse(cookie.getSecure());
        }
    }

    @Nested
    class RemoveAuthorizationRequest {

        @Test
        void shouldReturnNullWhenNoCookiesExist() {
            when(httpServletRequest.getCookies()).thenReturn(null);

            OAuth2AuthorizationRequest result = store.removeAuthorizationRequest(httpServletRequest, httpServletResponse);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenCookieExistsButCannotBeDeserialized() {
            Cookie cookie = new Cookie("oauth2_auth_request", "broken_data");
            when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

            OAuth2AuthorizationRequest result = store.removeAuthorizationRequest(httpServletRequest, httpServletResponse);

            assertNull(result);
        }

        @Test
        void shouldAlwaysRemoveCookieEvenWhenNoAuthorizationRequestExists() {
            when(httpServletRequest.getCookies()).thenReturn(null);

            store.removeAuthorizationRequest(httpServletRequest, httpServletResponse);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(httpServletResponse, times(2)).addCookie(captor.capture());

            assertTrue(captor.getAllValues().stream().anyMatch(Cookie::getSecure));
            assertTrue(captor.getAllValues().stream().anyMatch(cookie -> !cookie.getSecure()));
            captor.getAllValues().forEach(cookie -> {
                assertEquals("", cookie.getValue());
                assertEquals(0, cookie.getMaxAge());
            });
        }
    }
}
