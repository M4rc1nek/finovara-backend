package com.finovara.finovarabackend.security.jwt;

import com.finovara.finovarabackend.security.SecurityProperties;
import com.finovara.finovarabackend.security.oauth2.OAuth2AccessTokenCookie;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtOAuth2AuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;

    private JwtOAuth2AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setWhitelist(List.of("/api/auth/**"));
        filter = new JwtOAuth2AuthenticationFilter(jwtService, userRepository, securityProperties, new JwtTokenResolver());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBlockProtectedEndpointWhenOAuth2PasswordIsNotSet() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user(false);

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isEqualTo("Password setup required");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAllowPasswordSetupEndpointWhenOAuth2PasswordIsNotSet() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedRequest("POST", "/api/auth/set-password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user(false);

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void shouldAllowLoginEndpointWhenOAuth2PasswordIsNotSet() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user(false);

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAllowRegisterEndpointWhenOAuth2PasswordIsNotSet() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedRequest("POST", "/api/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user(false);

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAllowProtectedEndpointWhenPasswordIsSet() throws ServletException, IOException {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user(true);

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void shouldAuthenticateWithOAuth2AccessTokenCookie() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard");
        request.setCookies(new Cookie(OAuth2AccessTokenCookie.COOKIE_NAME, "jwt-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user(true);

        when(jwtService.isTokenValid("jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    private MockHttpServletRequest authenticatedRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer jwt-token");
        return request;
    }

    private User user(boolean passwordSet) {
        return User.builder()
                .id(1L)
                .email("jane@example.com")
                .passwordSet(passwordSet)
                .build();
    }
}
