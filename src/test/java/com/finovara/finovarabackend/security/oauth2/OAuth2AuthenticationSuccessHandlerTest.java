/*
package com.finovara.finovarabackend.security.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.finovara.finovarabackend.security.jwt.JwtService;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private GoogleOAuth2UserService googleOAuth2UserSynchronizationService;
    @Mock
    private JwtService jwtService;
    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
                googleOAuth2UserSynchronizationService,
                jwtService,
                objectMapper,
                authorizationRequestRepository
        );
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnJwtTokenAfterSuccessfulGoogleAuthentication() throws Exception {
        OAuth2User oauth2User = googleUser();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(oauth2User, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = User.builder()
                .id(12L)
                .username("Jane Doe")
                .email("jane@example.com")
                .profileImagePath("https://lh3.googleusercontent.com/avatar")
                .build();

        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(googleOAuth2UserSynchronizationService.synchronize(oauth2User)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("finovara-jwt");

        handler.onAuthenticationSuccess(request, response, authentication);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(body.get("id").asLong()).isEqualTo(12L);
        assertThat(body.get("username").asText()).isEqualTo("Jane Doe");
        assertThat(body.get("email").asText()).isEqualTo("jane@example.com");
        assertThat(body.get("profileImageUrl").asText()).isEqualTo("https://lh3.googleusercontent.com/avatar");
        assertThat(body.get("jwtToken").asText()).isEqualTo("finovara-jwt");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);
    }

    @Test
    void shouldReturnConflictWhenGoogleEmailAlreadyExists() throws Exception {
        OAuth2User oauth2User = googleUser();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(oauth2User, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(googleOAuth2UserSynchronizationService.synchronize(oauth2User))
                .thenThrow(new EmailAlreadyExistsException("User already exists"));

        handler.onAuthenticationSuccess(request, response, authentication);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.get("message").asText()).isEqualTo("User already exists");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);
    }

    private OAuth2User googleUser() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-123",
                "email", "jane@example.com",
                "name", "Jane Doe",
                "picture", "https://lh3.googleusercontent.com/avatar"
        );

        return new DefaultOAuth2User(List.of(), attributes, "sub");
    }
}
*/
