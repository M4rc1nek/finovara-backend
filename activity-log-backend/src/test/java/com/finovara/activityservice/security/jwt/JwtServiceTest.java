package com.finovara.activityservice.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", JwtTestTokenFactory.SECRET);
    }

    @Test
    void shouldExtractUserId() {
        String token = validToken(42L);

        Long userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void shouldValidateActiveToken() {
        assertThat(jwtService.isTokenValid(validToken(42L))).isTrue();
    }

    @Test
    void shouldDetectExpiredToken() {
        String expiredToken = JwtTestTokenFactory.token(
                42L,
                Date.from(Instant.now().minusSeconds(120)),
                Date.from(Instant.now().minusSeconds(60))
        );

        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    private String validToken(Long userId) {
        return JwtTestTokenFactory.token(
                userId,
                Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(3600))
        );
    }
}
