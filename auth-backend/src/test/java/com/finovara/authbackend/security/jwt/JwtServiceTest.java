package com.finovara.authbackend.security.jwt;

import com.finovara.authbackend.security.jwt.logout.JwtBlacklistService;
import com.finovara.authbackend.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtBlacklistService jwtBlacklistService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtBlacklistService = new JwtBlacklistService();
        jwtService = new JwtService(jwtBlacklistService);
        ReflectionTestUtils.setField(jwtService, "secretKey", base64Secret());
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    @Nested
    class ValidationTests {

        @Test
        void shouldValidateGeneratedToken() {
            String token = jwtService.generateToken(user());

            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        void shouldRejectBlacklistedToken() {
            String token = jwtService.generateToken(user());

            jwtBlacklistService.blacklist(token, Instant.now().plusSeconds(60));

            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }

    private User user() {
        return User.builder()
                .id(1L)
                .passwordSet(true)
                .build();
    }

    private String base64Secret() {
        byte[] secret = "0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(secret);
    }
}
