package com.finovara.authbackend.security.jwt.logout;

import com.finovara.authbackend.security.jwt.JwtService;
import com.finovara.authbackend.security.jwt.JwtTokenResolver;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private JwtTokenResolver jwtTokenResolver;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtBlacklistService jwtBlacklistService;
    @Mock
    private HttpServletRequest request;

    private MockHttpServletResponse response;
    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        logoutService = new LogoutService(jwtTokenResolver, jwtService, jwtBlacklistService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class LogoutTests {

        @Test
        void shouldBlacklistResolvedTokenUntilJwtExpiration() {
            Instant expiresAt = Instant.parse("2026-05-17T11:00:00Z");

            when(jwtTokenResolver.resolve(request)).thenReturn(Optional.of("jwt-token"));
            when(jwtService.extractExpiration("jwt-token")).thenReturn(Date.from(expiresAt));

            logoutService.logout(request, response);

            verify(jwtBlacklistService).blacklist("jwt-token", expiresAt);
        }

        @Test
        void shouldDoNothingWhenTokenIsMissing() {
            when(jwtTokenResolver.resolve(request)).thenReturn(Optional.empty());

            logoutService.logout(request, response);

            verify(jwtBlacklistService, never()).blacklist(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        }

        @Test
        void shouldIgnoreInvalidToken() {
            when(jwtTokenResolver.resolve(request)).thenReturn(Optional.of("invalid-token"));
            when(jwtService.extractExpiration("invalid-token")).thenThrow(new JwtException("Invalid token"));

            logoutService.logout(request, response);

            verify(jwtBlacklistService, never()).blacklist(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        }

        @Test
        void shouldClearOAuth2CookieAndSecurityContext() {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", null));
            when(jwtTokenResolver.resolve(request)).thenReturn(Optional.empty());

            logoutService.logout(request, response);

            assertThat(response.getCookies()).hasSize(2);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
