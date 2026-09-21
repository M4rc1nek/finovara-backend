package com.finovara.authservice.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Component
public class OAuth2PendingLoginCookie {

    public static final String COOKIE_NAME = "oauth2_pending_login";

    private static final String SALT = "5c0744940b5c369b";

    private final TextEncryptor encryptor;
    private final long ttlSeconds;

    public OAuth2PendingLoginCookie(
            @Value("${oauth2.pending-login.secret}") String secret,
            @Value("${oauth2.pending-login.ttl-seconds:600}") long ttlSeconds) {
        this.encryptor = Encryptors.delux(secret, SALT);
        this.ttlSeconds = ttlSeconds;
    }

    public record PendingLogin(Long userId, String sourceEventId) {
    }

    public void add(HttpServletResponse response, Long userId, String sourceEventId, boolean secure) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        addCookie(response, encryptor.encrypt(userId + ":" + sourceEventId + ":" + expiresAt), (int) ttlSeconds, secure);
    }

    public Optional<PendingLogin> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .findFirst()
                .flatMap(cookie -> decrypt(cookie.getValue()));
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, "", 0, false);
        addCookie(response, "", 0, true);
    }

    private Optional<PendingLogin> decrypt(String value) {
        try {
            String[] parts = encryptor.decrypt(value).split(":");
            if (parts.length != 3 || Instant.now().getEpochSecond() > Long.parseLong(parts[2])) {
                return Optional.empty();
            }
            return Optional.of(new PendingLogin(Long.parseLong(parts[0]), parts[1]));
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void addCookie(HttpServletResponse response, String value, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}