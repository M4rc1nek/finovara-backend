package com.finovara.authservice.security.jwt.logout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String PREFIX = "jwt:blacklist:";

    private JwtBlacklistService jwtBlacklistService;

    @BeforeEach
    void setUp() {
        jwtBlacklistService = new JwtBlacklistService(redisTemplate);
    }

    @Nested
    class BlacklistTests {

        @Test
        void shouldBlacklistTokenUntilExpiration() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String token = "jwt-token";

            jwtBlacklistService.blacklist(token, Instant.now().plusSeconds(60));

            verify(valueOperations).set(eq(PREFIX + token), eq("1"), any(Duration.class));
        }

        @Test
        void shouldIgnoreExpiredToken() {
            jwtBlacklistService.blacklist("jwt-token", Instant.now().minusSeconds(1));

            verifyNoInteractions(redisTemplate);
        }

        @Test
        void shouldIgnoreBlankToken() {
            jwtBlacklistService.blacklist(" ", Instant.now().plusSeconds(60));

            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    class IsBlacklistedTests {

        @Test
        void shouldReturnTrueWhenTokenIsBlacklisted() {
            String token = "jwt-token";
            when(redisTemplate.hasKey(PREFIX + token)).thenReturn(Boolean.TRUE);

            assertThat(jwtBlacklistService.isBlacklisted(token)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenTokenNotInRedis() {
            String token = "jwt-token";
            when(redisTemplate.hasKey(PREFIX + token)).thenReturn(Boolean.FALSE);

            assertThat(jwtBlacklistService.isBlacklisted(token)).isFalse();
        }

        @Test
        void shouldReturnFalseForBlankToken() {
            assertThat(jwtBlacklistService.isBlacklisted(" ")).isFalse();
            verifyNoInteractions(redisTemplate);
        }
    }
}