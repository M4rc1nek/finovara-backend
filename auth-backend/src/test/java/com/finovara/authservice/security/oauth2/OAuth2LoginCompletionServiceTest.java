package com.finovara.authservice.security.oauth2;

import com.finovara.authservice.exception.unauthorized.InvalidCredentialsException;
import com.finovara.authservice.riskverification.service.RiskGuardService;
import com.finovara.authservice.security.jwt.JwtService;
import com.finovara.authservice.security.oauth2.OAuth2PendingLoginCookie.PendingLogin;
import com.finovara.authservice.security.oauth2.dto.OAuth2LoginResponseDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginCompletionServiceTest {

    @Mock
    private OAuth2PendingLoginCookie pendingLoginCookie;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private RiskGuardService riskGuardService;

    @Mock
    private JwtService jwtService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private OAuth2LoginCompletionService oAuth2LoginCompletionService;

    private static final Long USER_ID = 1L;
    private static final String SOURCE_EVENT_ID = "source-event-id";
    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String JWT_TOKEN = "mock-jwt-token";

    private User user;
    private PendingLogin pendingLogin;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .email(EMAIL)
                .profileImagePath(null)
                .passwordSet(true)
                .build();
        pendingLogin = new PendingLogin(USER_ID, SOURCE_EVENT_ID);
    }

    private void stubValidRequest() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0");
    }

    private void stubHappyPath() {
        when(pendingLoginCookie.read(request)).thenReturn(Optional.of(pendingLogin));
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(JWT_TOKEN);
        when(request.isSecure()).thenReturn(true);
        stubValidRequest();
    }

    @Nested
    class PendingLoginResolution {

        @Test
        void shouldThrowInvalidCredentialsExceptionWhenPendingLoginCookieMissing() {
            when(pendingLoginCookie.read(request)).thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class, () -> oAuth2LoginCompletionService.complete(request, response));

            verifyNoInteractions(userManagerService, riskGuardService, jwtService, kafkaTemplate);
        }

        @Test
        void shouldFetchUserByPendingLoginUserIdWhenCookiePresent() {
            stubHappyPath();

            oAuth2LoginCompletionService.complete(request, response);

            verify(userManagerService).getUserByIdOrThrow(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(pendingLoginCookie.read(request)).thenReturn(Optional.of(pendingLogin));
            when(userManagerService.getUserByIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> oAuth2LoginCompletionService.complete(request, response));

            verifyNoInteractions(riskGuardService, jwtService, kafkaTemplate);
            verify(pendingLoginCookie, never()).clear(any());
        }
    }

    @Nested
    class RiskGuarding {

        @Test
        void shouldGuardAgainstRiskWithUserIdEmailAndSourceEventIdWhenCompletingLogin() {
            stubHappyPath();

            oAuth2LoginCompletionService.complete(request, response);

            verify(riskGuardService).guard(USER_ID, RiskTriggerType.LOGIN, EMAIL, SOURCE_EVENT_ID, request);
        }

        @Test
        void shouldNotPublishLoginActivityWhenRiskGuardFails() {
            when(pendingLoginCookie.read(request)).thenReturn(Optional.of(pendingLogin));
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            doThrow(new RuntimeException("Risk detected"))
                    .when(riskGuardService).guard(USER_ID, RiskTriggerType.LOGIN, EMAIL, SOURCE_EVENT_ID, request);

            assertThrows(RuntimeException.class, () -> oAuth2LoginCompletionService.complete(request, response));

            verifyNoInteractions(kafkaTemplate, jwtService);
        }

        @Test
        void shouldNotAddAccessTokenCookieWhenRiskGuardFails() {
            when(pendingLoginCookie.read(request)).thenReturn(Optional.of(pendingLogin));
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            doThrow(new RuntimeException("Risk detected"))
                    .when(riskGuardService).guard(USER_ID, RiskTriggerType.LOGIN, EMAIL, SOURCE_EVENT_ID, request);

            assertThrows(RuntimeException.class, () -> oAuth2LoginCompletionService.complete(request, response));

            verify(response, never()).addCookie(any(Cookie.class));
            verify(pendingLoginCookie, never()).clear(any());
        }
    }

    @Nested
    class LoginActivityPublishing {

        @Test
        void shouldPublishLoginActivityEventWithSuccessfulStatusWhenCompletingLogin() {
            stubHappyPath();

            oAuth2LoginCompletionService.complete(request, response);

            ArgumentCaptor<LoginActivityEvent> eventCaptor = ArgumentCaptor.forClass(LoginActivityEvent.class);
            verify(kafkaTemplate).send(eq("user.logged-in"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().status()).isEqualTo(LoginActivityStatus.SUCCESSFUL);
        }

        @Test
        void shouldPublishLoginActivityEventWithCorrectUserIdWhenCompletingLogin() {
            stubHappyPath();

            oAuth2LoginCompletionService.complete(request, response);

            ArgumentCaptor<LoginActivityEvent> eventCaptor = ArgumentCaptor.forClass(LoginActivityEvent.class);
            verify(kafkaTemplate).send(eq("user.logged-in"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    class AccessTokenCookieHandling {

        @Test
        void shouldAddAccessTokenCookieWithGeneratedTokenWhenCompletingLogin() {
            stubHappyPath();

            oAuth2LoginCompletionService.complete(request, response);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(cookieCaptor.capture());

            Cookie cookie = cookieCaptor.getValue();
            assertThat(cookie.getName()).isEqualTo(OAuth2AccessTokenCookie.COOKIE_NAME);
            assertThat(cookie.getValue()).isEqualTo(JWT_TOKEN);
        }

        @Test
        void shouldAddSecureAccessTokenCookieWhenRequestIsSecure() {
            stubHappyPath();

            oAuth2LoginCompletionService.complete(request, response);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(cookieCaptor.capture());
            assertThat(cookieCaptor.getValue().getSecure()).isTrue();
        }

        @Test
        void shouldAddNonSecureAccessTokenCookieWhenRequestIsNotSecure() {
            when(pendingLoginCookie.read(request)).thenReturn(Optional.of(pendingLogin));
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(jwtService.generateToken(user)).thenReturn(JWT_TOKEN);
            when(request.isSecure()).thenReturn(false);
            stubValidRequest();

            oAuth2LoginCompletionService.complete(request, response);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(cookieCaptor.capture());
            assertThat(cookieCaptor.getValue().getSecure()).isFalse();
        }
    }

    @Nested
    class PendingLoginCookieCleanup {

        @Test
        void shouldClearPendingLoginCookieWhenCompletingLogin() {
            stubHappyPath();

            oAuth2LoginCompletionService.complete(request, response);

            verify(pendingLoginCookie).clear(response);
        }
    }

    @Nested
    class ResponseDtoConstruction {

        @Test
        void shouldReturnResponseDtoWithUserIdUsernameAndEmailWhenCompletingLogin() {
            stubHappyPath();

            OAuth2LoginResponseDto result = oAuth2LoginCompletionService.complete(request, response);

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.username()).isEqualTo(USERNAME);
            assertThat(result.email()).isEqualTo(EMAIL);
        }

        @Test
        void shouldReturnResponseDtoWithPasswordSetTrueWhenUserHasPasswordSet() {
            stubHappyPath();

            OAuth2LoginResponseDto result = oAuth2LoginCompletionService.complete(request, response);

            assertThat(result.passwordSet()).isTrue();
        }

        @Test
        void shouldReturnResponseDtoWithPasswordSetFalseWhenUserHasNoPasswordSet() {
            user = User.builder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .email(EMAIL)
                    .profileImagePath(null)
                    .passwordSet(false)
                    .build();
            stubHappyPath();

            OAuth2LoginResponseDto result = oAuth2LoginCompletionService.complete(request, response);

            assertThat(result.passwordSet()).isFalse();
        }
    }
}