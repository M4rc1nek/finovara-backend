package com.finovara.authservice.settings.security.twofactor.service;

import com.finovara.authservice.settings.security.twofactor.cookie.TwoFactorPendingLoginCookie;
import com.finovara.authservice.settings.security.twofactor.cookie.TokenDelivery;
import com.finovara.authservice.settings.security.twofactor.service.code.TwoFactorSecretEncryption;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorPendingLoginCookieTest {

    private static final String ENCRYPTION_SECRET = "test-secret-1234567890";
    private static final String ENCRYPTION_SALT = "5c0744940b5c369b";
    private static final String SOURCE_EVENT_ID = "source:event:id";
    private static final String COOKIE_NAME = "two_factor_pending_login";

    private TwoFactorPendingLoginCookie pendingLoginCookie;

    @BeforeEach
    void setUp() {
        TwoFactorSecretEncryption encryption = new TwoFactorSecretEncryption();
        ReflectionTestUtils.setField(encryption, "encryptionSecret", ENCRYPTION_SECRET);
        ReflectionTestUtils.setField(encryption, "encryptionSalt", ENCRYPTION_SALT);
        ReflectionTestUtils.invokeMethod(encryption, "initialize");

        pendingLoginCookie = new TwoFactorPendingLoginCookie(encryption);
        ReflectionTestUtils.setField(pendingLoginCookie, "ttlSeconds", 600L);
    }

    @Test
    void shouldEncryptPendingLoginAndPreserveDeliveryMode() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        pendingLoginCookie.add(response, 42L, SOURCE_EVENT_ID, TokenDelivery.COOKIE, true);

        Cookie cookie = response.getCookie(COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).doesNotContain("42:", SOURCE_EVENT_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);

        assertThat(pendingLoginCookie.read(request))
                .contains(new TwoFactorPendingLoginCookie.PendingLogin(
                        42L, SOURCE_EVENT_ID, TokenDelivery.COOKIE));
    }

    @Test
    void shouldReadPasswordLoginDeliveryMode() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        pendingLoginCookie.add(response, 42L, SOURCE_EVENT_ID, TokenDelivery.RESPONSE, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(response.getCookie(COOKIE_NAME));

        assertThat(pendingLoginCookie.read(request))
                .contains(new TwoFactorPendingLoginCookie.PendingLogin(
                        42L, SOURCE_EVENT_ID, TokenDelivery.RESPONSE));
    }

    @Test
    void shouldReturnEmptyWhenCookieIsNotPresent() {
        assertThat(pendingLoginCookie.read(new MockHttpServletRequest())).isEmpty();
    }
}
