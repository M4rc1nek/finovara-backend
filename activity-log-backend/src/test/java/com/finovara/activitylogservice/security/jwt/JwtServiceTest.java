package com.finovara.activitylogservice.security.jwt;

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
        ReflectionTestUtils.setField(jwtService, "secretKey", JwtTokenFactoryTest.SECRET);
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
        String expiredToken = JwtTokenFactoryTest.token(
                42L,
                Date.from(Instant.now().minusSeconds(120)),
                Date.from(Instant.now().minusSeconds(60))
        );

        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    private String validToken(Long userId) {
        return JwtTokenFactoryTest.token(
                userId,
                Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(3600))
        );
    }
}
