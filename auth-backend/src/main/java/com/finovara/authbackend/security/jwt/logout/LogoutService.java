package com.finovara.authbackend.security.jwt.logout;

import com.finovara.authbackend.security.jwt.JwtService;
import com.finovara.authbackend.security.jwt.JwtTokenResolver;
import com.finovara.authbackend.security.oauth2.OAuth2AccessTokenCookie;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService {

    private final JwtTokenResolver jwtTokenResolver;
    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        jwtTokenResolver.resolve(request).ifPresent(this::blacklist);
        OAuth2AccessTokenCookie.clear(response);
        SecurityContextHolder.clearContext();
        invalidateSession(request);
        log.info("User logged out successfully. Security context cleared and cookies removed.");
    }

    private void blacklist(String token) {
        try {
            Instant expiresAt = jwtService.extractExpiration(token).toInstant();
            jwtBlacklistService.blacklist(token, expiresAt);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("Skipping JWT blacklist during logout: {}", exception.getMessage());
        }
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
