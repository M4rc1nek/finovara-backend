package com.finovara.activitylogservice.security.jwt;

import com.finovara.activitylogservice.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtTokenResolver jwtTokenResolver;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, jwtTokenResolver);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenResolver.resolve(request)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilter(request, response, terminalAuthorizationChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldPassRequestWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtTokenResolver.resolve(request)).thenReturn(Optional.of("valid-token"));
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(42L);

        jwtAuthenticationFilter.doFilter(request, response, terminalAuthorizationChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal())
                .isInstanceOfSatisfying(CustomUserDetails.class, user -> assertThat(user.getId()).isEqualTo(42L));
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestHasExpiredToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer expired-token");

        when(jwtTokenResolver.resolve(request)).thenReturn(Optional.of("expired-token"));
        when(jwtService.isTokenValid("expired-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, terminalAuthorizationChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private FilterChain terminalAuthorizationChain() {
        return (request, response) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            httpResponse.setStatus(HttpServletResponse.SC_OK);
        };
    }
}
