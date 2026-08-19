package com.finovara.authservice.security.oauth2;

import com.finovara.contracts.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.user.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private GoogleOAuth2UserService googleOAuth2UserService;

    @Mock
    private JwtService jwtService;

    @Mock
    private OAuth2AuthorizationRequestCookieStore authorizationRequestRepository;


    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .profileImagePath(null)
                .passwordSet(false)
                .build();
    }

    private void stubSuccessfulFlow(Object principal) throws IOException {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(googleOAuth2UserService.synchronize(any(OAuth2User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(mockUser)).thenReturn("mock-jwt-token");
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");
    }

    @Nested
    class PrincipalTypeHandling {

        @Test
        void shouldHandleDefaultOidcUserPrincipal() throws Exception {
            DefaultOidcUser oidcUser = mock(DefaultOidcUser.class);
            stubSuccessfulFlow(oidcUser);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(googleOAuth2UserService).synchronize(oidcUser);
        }

        @Test
        void shouldHandleGenericOAuth2UserPrincipal() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(googleOAuth2UserService).synchronize(oauth2User);
        }

        @Test
        void shouldRedirectToErrorWhenPrincipalTypeIsUnsupported() throws Exception {
            when(authentication.getPrincipal()).thenReturn("unsupported-principal");

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("error=oauth2_authentication_failed");
        }
    }

    @Nested
    class RedirectUrlConstruction {

        @Test
        void shouldSetJwtTokenInHttpOnlyCookie() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(httpServletResponse).addCookie(cookieCaptor.capture());

            Cookie cookie = cookieCaptor.getValue();
            assertThat(cookie.getName()).isEqualTo(OAuth2AccessTokenCookie.COOKIE_NAME);
            assertThat(cookie.getValue()).isEqualTo("mock-jwt-token");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getMaxAge()).isEqualTo(86400);
        }

        @Test
        void shouldNotIncludeJwtTokenInRedirectUrl() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).doesNotContain("token=");
        }

        @Test
        void shouldIncludeUserIdInRedirectUrl() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("id=1");
        }

        @Test
        void shouldIncludeUsernameInRedirectUrl() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("username=testuser");
        }

        @Test
        void shouldIncludeEmailInRedirectUrl() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("email=test@example.com");
        }

        @Test
        void shouldIncludePasswordSetInRedirectUrl() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("passwordSet=false");
        }

        @Test
        void shouldIncludeEmptyProfileImageUrlWhenPathIsNull() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            mockUser.setProfileImagePath(null);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("profileImageUrl=");
        }

        @Test
        void shouldRedirectToOAuth2SuccessEndpoint() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).startsWith("https://localhost:5173/oauth2/success");
        }
    }

    @Nested
    class CleanupBehaviour {

        @Test
        void shouldInvalidateSessionOnSuccess() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(httpSession).invalidate();
        }

        @Test
        void shouldNotThrowWhenNoSessionExists() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            // use the standard successful flow setup but simulate no session
            stubSuccessfulFlow(oauth2User);
            when(httpServletRequest.getSession(false)).thenReturn(null);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(httpSession, never()).invalidate();
        }

        @Test
        void shouldRemoveAuthorizationRequestCookie() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(authorizationRequestRepository).removeAuthorizationRequest(httpServletRequest, httpServletResponse);
        }

        @Test
        void shouldClearSecurityContext() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            SecurityContext spyContext = mock(SecurityContext.class);
            SecurityContextHolder.setContext(spyContext);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

            SecurityContextHolder.clearContext();
        }
    }

    @Nested
    class LoginActivityRecording {

        @Test
        void shouldRecordSuccessfulLoginActivityOnSuccess() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<LoginActivityEvent> eventCaptor = ArgumentCaptor.forClass(LoginActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.login"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().status()).isEqualTo(LoginActivityStatus.SUCCESSFUL);
        }

        @Test
        void shouldRecordLoginActivityWithCorrectUserId() throws Exception {
            mockUser = User.builder()
                    .id(42L)
                    .username("anotheruser")
                    .email("another@example.com")
                    .profileImagePath(null)
                    .passwordSet(true)
                    .build();

            OAuth2User oauth2User = mock(OAuth2User.class);
            // reuse helper to configure request/jwt/synchronization for successful flow
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<LoginActivityEvent> eventCaptor = ArgumentCaptor.forClass(LoginActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.login"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(42L);
        }

        @Test
        void shouldNotRecordLoginActivityOnBusinessException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any()))
                    .thenThrow(new EntityAlreadyExistsException("email_already_exists"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldNotRecordLoginActivityOnRuntimeException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any()))
                    .thenThrow(new RuntimeException("unexpected"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldNotRecordLoginActivityOnUnsupportedPrincipal() throws Exception {
            when(authentication.getPrincipal()).thenReturn("unsupported-principal");

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(kafkaTemplate);
        }
    }

    @Nested
    class BusinessExceptionHandling {

        @Test
        void shouldRedirectWithMessageOnEmailAlreadyExistsException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            EntityAlreadyExistsException ex = new EntityAlreadyExistsException("email_already_exists");
            when(googleOAuth2UserService.synchronize(any())).thenThrow(ex);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("error=email_already_exists");
        }

        @Test
        void shouldRedirectWithMessageOnNameAlreadyExistsException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            EntityAlreadyExistsException ex = new EntityAlreadyExistsException("name_already_exists");
            when(googleOAuth2UserService.synchronize(any())).thenThrow(ex);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("error=name_already_exists");
        }

        @Test
        void shouldRedirectWithMessageOnInvalidInputException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            InvalidInputException ex = new InvalidInputException("invalid_input");
            when(googleOAuth2UserService.synchronize(any())).thenThrow(ex);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("error=invalid_input");
        }

        @Test
        void shouldRedirectToAuthPageOnBusinessException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any())).thenThrow(new EntityAlreadyExistsException("err"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).startsWith("https://localhost:5173/auth");
        }
    }

    @Nested
    class GenericRuntimeExceptionHandling {

        @Test
        void shouldRedirectWithGenericErrorOnRuntimeException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any())).thenThrow(new RuntimeException("unexpected"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).contains("error=oauth2_authentication_failed");
        }

        @Test
        void shouldRedirectToAuthPageOnRuntimeException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any())).thenThrow(new RuntimeException("unexpected"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpServletResponse).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue()).startsWith("https://localhost:5173/auth");
        }
    }
}