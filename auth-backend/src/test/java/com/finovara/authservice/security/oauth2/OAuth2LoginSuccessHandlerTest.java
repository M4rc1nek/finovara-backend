package com.finovara.authservice.security.oauth2;

import com.finovara.authservice.user.model.User;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private GoogleOAuth2UserService googleOAuth2UserService;

    @Mock
    private OAuth2AuthorizationRequestCookieStore authorizationRequestRepository;

    @Mock
    private OAuth2PendingLoginCookie pendingLoginCookie;

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

    private void stubSuccessfulFlow(Object principal) {
        when(authentication.getPrincipal()).thenReturn(principal);
        when(googleOAuth2UserService.synchronize(any(OAuth2User.class))).thenReturn(mockUser);
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);
        when(httpServletRequest.isSecure()).thenReturn(true);
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

        @Test
        void shouldNotSynchronizeUserWhenPrincipalTypeIsUnsupported() throws Exception {
            when(authentication.getPrincipal()).thenReturn("unsupported-principal");

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(googleOAuth2UserService, pendingLoginCookie);
        }
    }

    @Nested
    class PendingLoginCookieHandling {

        @Test
        void shouldAddPendingLoginCookieWithUserIdWhenAuthenticationSucceeds() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(pendingLoginCookie).add(eq(httpServletResponse), eq(1L), anyString(), eq(true));
        }

        @Test
        void shouldAddPendingLoginCookieWithSecureFlagMatchingRequestWhenRequestIsNotSecure() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any(OAuth2User.class))).thenReturn(mockUser);
            when(httpServletRequest.getSession(false)).thenReturn(httpSession);
            when(httpServletRequest.isSecure()).thenReturn(false);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(pendingLoginCookie).add(eq(httpServletResponse), eq(1L), anyString(), eq(false));
        }

        @Test
        void shouldNotAddPendingLoginCookieWhenSynchronizeFails() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any())).thenThrow(new RuntimeException("unexpected"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(pendingLoginCookie);
        }
    }

    @Nested
    class RedirectUrlConstruction {

        @Test
        void shouldRedirectToOAuth2VerifyEndpointOnSuccess() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            stubSuccessfulFlow(oauth2User);

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verify(httpServletResponse).sendRedirect("https://localhost:5173/oauth2/verify");
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

        @Test
        void shouldNotRemoveAuthorizationRequestCookieWhenSynchronizeFailsWithBusinessException() throws Exception {
            OAuth2User oauth2User = mock(OAuth2User.class);
            when(authentication.getPrincipal()).thenReturn(oauth2User);
            when(googleOAuth2UserService.synchronize(any())).thenThrow(new EntityAlreadyExistsException("err"));

            oAuth2LoginSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);

            verifyNoInteractions(authorizationRequestRepository);
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