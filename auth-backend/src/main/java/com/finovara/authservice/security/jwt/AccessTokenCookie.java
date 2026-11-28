package com.finovara.authservice.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class AccessTokenCookie {

    public static final String COOKIE_NAME = "oauth2_access_token";
    public static final String ALTERNATE_COOKIE_NAME = "access_token";

    private static final int MAX_AGE_SECONDS = 24 * 60 * 60;

    private AccessTokenCookie() {
    }

    public static void add(HttpServletResponse response, String token, boolean secure) {
        writeCookie(response, token, MAX_AGE_SECONDS, secure);
    }

    public static void clear(HttpServletResponse response) {
        writeCookie(response, "", 0, false);
        writeCookie(response, "", 0, true);
        writeCookie(response, ALTERNATE_COOKIE_NAME, "", 0, false);
        writeCookie(response, ALTERNATE_COOKIE_NAME, "", 0, true);
    }

    private static void writeCookie(HttpServletResponse response, String value, int maxAge, boolean secure) {
        writeCookie(response, COOKIE_NAME, value, maxAge, secure);
    }

    private static void writeCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAge,
            boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
