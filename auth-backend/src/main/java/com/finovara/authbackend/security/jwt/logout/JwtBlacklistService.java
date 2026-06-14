package com.finovara.authbackend.security.jwt.logout;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtBlacklistService {

    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklist(String token, Instant expiresAt) {
        if (!StringUtils.hasText(token) || expiresAt == null || isExpired(expiresAt)) {
            return;
        }

        removeExpiredTokens();
        blacklistedTokens.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        Instant expiresAt = blacklistedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }

        if (isExpired(expiresAt)) {
            blacklistedTokens.remove(token, expiresAt);
            return false;
        }

        return true;
    }

    private void removeExpiredTokens() {
        blacklistedTokens.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private boolean isExpired(Instant expiresAt) {
        return !expiresAt.isAfter(Instant.now());
    }
}
