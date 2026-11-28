package com.finovara.authservice.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class JwtTokenResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public Optional<String> resolve(HttpServletRequest request) {
        return extractBearerToken(request)
                .or(() -> extractAccessTokenCookie(request));
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private Optional<String> extractAccessTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> AccessTokenCookie.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .or(() -> Arrays.stream(cookies)
                        .filter(cookie -> AccessTokenCookie.ALTERNATE_COOKIE_NAME.equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst());
    }
}
