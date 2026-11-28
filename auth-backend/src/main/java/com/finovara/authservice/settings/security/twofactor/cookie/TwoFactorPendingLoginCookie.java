package com.finovara.authservice.settings.security.twofactor.cookie;

import com.finovara.authservice.settings.security.twofactor.service.code.TwoFactorSecretEncryption;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TwoFactorPendingLoginCookie {

    private static final String COOKIE_NAME = "two_factor_pending_login";

    private final TwoFactorSecretEncryption encryption;

    @Value("${application.security.two-factor.pending-login-ttl-seconds}")
    private long ttlSeconds;

    public record PendingLogin(Long userId, String sourceEventId, TokenDelivery tokenDelivery) {
    }

    public void add(HttpServletResponse response, Long userId, String sourceEventId, TokenDelivery tokenDelivery, boolean secure) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String encodedSourceEventId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sourceEventId.getBytes(StandardCharsets.UTF_8));
        String payload = userId + ":" + encodedSourceEventId + ":" + expiresAt + ":" + tokenDelivery.name();
        writeCookie(response, encryption.encrypt(payload), (int) ttlSeconds, secure);
    }

    public Optional<PendingLogin> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .findFirst()
                .flatMap(cookie -> decryptPendingLogin(cookie.getValue()));
    }

    public void clear(HttpServletResponse response) {
        writeCookie(response, "", 0, false);
        writeCookie(response, "", 0, true);
    }

    private Optional<PendingLogin> decryptPendingLogin(String value) {
        try {
            String[] parts = encryption.decrypt(value).split(":");
            if (parts.length != 4 || Instant.now().getEpochSecond() > Long.parseLong(parts[2])) {
                return Optional.empty();
            }
            return Optional.of(new PendingLogin(
                    Long.parseLong(parts[0]),
                    new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
                    TokenDelivery.valueOf(parts[3])));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return Optional.empty();
        }
    }

    private void writeCookie(HttpServletResponse response, String value, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
