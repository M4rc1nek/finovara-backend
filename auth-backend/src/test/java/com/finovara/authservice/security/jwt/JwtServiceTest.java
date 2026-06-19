package com.finovara.authservice.security.jwt;

import com.finovara.authservice.security.jwt.logout.JwtBlacklistService;
import com.finovara.authservice.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(jwtBlacklistService);
        ReflectionTestUtils.setField(jwtService, "secretKey", base64Secret());
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    @Nested
    class ValidationTests {

        @Test
        void shouldValidateGeneratedToken() {
            String token = jwtService.generateToken(user());
            when(jwtBlacklistService.isBlacklisted(token)).thenReturn(false);

            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        void shouldRejectBlacklistedToken() {
            String token = jwtService.generateToken(user());
            when(jwtBlacklistService.isBlacklisted(token)).thenReturn(true);

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