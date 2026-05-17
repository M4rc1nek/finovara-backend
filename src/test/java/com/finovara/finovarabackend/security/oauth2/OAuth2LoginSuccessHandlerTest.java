package com.finovara.finovarabackend.security.oauth2;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.security.jwt.JwtService;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
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
    private LoginActivityService loginActivityService;

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
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any())).thenReturn(mockUser);
            when(jwtService.generateToken(mockUser)).thenReturn("token");
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

            verify(loginActivityService).createLoginActivity(
                    mockUser.getId(),
                    LoginActivityStatus.SUCCESSFUL,
                    httpServletRequest
            );
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
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any())).thenReturn(mockUser);
            when(jwtService.generateToken(mockUser)).thenReturn("mock-jwt-token");
            when(httpServletRequest.getSession(false)).thenReturn(httpSession);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(loginActivityService).createLoginActivity(eq(42L), eq(LoginActivityStatus.SUCCESSFUL), eq(httpServletRequest));
        }

        @Test
        void shouldNotRecordLoginActivityOnBusinessException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any()))
                    .thenThrow(new EmailAlreadyExistsException("email_already_exists"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(loginActivityService);
        }

        @Test
        void shouldNotRecordLoginActivityOnRuntimeException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any()))
                    .thenThrow(new RuntimeException("unexpected"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(loginActivityService);
        }

        @Test
        void shouldNotRecordLoginActivityOnUnsupportedPrincipal() throws Exception {
            when(authentication.getPrincipal()).thenReturn("unsupported-principal");

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(loginActivityService);
        }
    }

    @Nested
    class BusinessExceptionHandling {

        @Test
        void shouldRedirectWithMessageOnEmailAlreadyExistsException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            EmailAlreadyExistsException ex = new EmailAlreadyExistsException("email_already_exists");
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
            NameAlreadyExistsException ex = new NameAlreadyExistsException("name_already_exists");
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
            when(googleOAuth2UserService.synchronize(any())).thenThrow(new EmailAlreadyExistsException("err"));

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