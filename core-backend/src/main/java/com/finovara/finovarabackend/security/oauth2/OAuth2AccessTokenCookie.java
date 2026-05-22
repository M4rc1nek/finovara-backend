package com.finovara.finovarabackend.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class OAuth2AccessTokenCookie {

    public static final String COOKIE_NAME = "oauth2_access_token";

    private static final int ACCESS_TOKEN_COOKIE_MAX_AGE_SECONDS = 24 * 60 * 60;

    private OAuth2AccessTokenCookie() {
    }

    public static void add(HttpServletResponse response, String token, boolean secure) {
        addCookie(response, token, ACCESS_TOKEN_COOKIE_MAX_AGE_SECONDS, secure);
    }

    public static void clear(HttpServletResponse response) {
        addCookie(response, "", 0, false);
        addCookie(response, "", 0, true);
    }

    private static void addCookie(HttpServletResponse response, String value, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
