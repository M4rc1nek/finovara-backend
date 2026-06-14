package com.finovara.authbackend.security.jwt.logout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtBlacklistServiceTest {

    private JwtBlacklistService jwtBlacklistService;

    @BeforeEach
    void setUp() {
        jwtBlacklistService = new JwtBlacklistService();
    }

    @Nested
    class BlacklistTests {

        @Test
        void shouldBlacklistTokenUntilExpiration() {
            jwtBlacklistService.blacklist("jwt-token", Instant.now().plusSeconds(60));

            assertThat(jwtBlacklistService.isBlacklisted("jwt-token")).isTrue();
        }

        @Test
        void shouldIgnoreExpiredToken() {
            jwtBlacklistService.blacklist("jwt-token", Instant.now().minusSeconds(1));

            assertThat(jwtBlacklistService.isBlacklisted("jwt-token")).isFalse();
        }

        @Test
        void shouldIgnoreBlankToken() {
            jwtBlacklistService.blacklist(" ", Instant.now().plusSeconds(60));

            assertThat(jwtBlacklistService.isBlacklisted(" ")).isFalse();
        }
    }

    @Nested
    class ExpirationTests {

        @Test
        void shouldRemoveExpiredTokenWhenCheckingBlacklist() {
            jwtBlacklistService.blacklist("jwt-token", Instant.now().plusSeconds(60));
            jwtBlacklistService.blacklist("expired-token", Instant.now().minusSeconds(1));

            assertThat(jwtBlacklistService.isBlacklisted("expired-token")).isFalse();
            assertThat(jwtBlacklistService.isBlacklisted("jwt-token")).isTrue();
        }
    }
}
