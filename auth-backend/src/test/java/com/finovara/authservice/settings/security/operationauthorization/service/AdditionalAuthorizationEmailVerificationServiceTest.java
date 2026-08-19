package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.settings.account.dto.AttemptsDto;
import com.finovara.authservice.settings.account.service.verification.VerificationCodeEmailSender;
import com.finovara.authservice.util.attempts.properties.VerificationCodeProperties;
import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeRequest;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationEmailCodeResponse;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.attempts.VerificationCodeAttemptsTemplate;
import com.finovara.authservice.util.attempts.VerificationCodeVerifier;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.clientdata.browser.UserBrowser;
import com.finovara.contracts.clientdata.ip.ClientIp;
import com.finovara.contracts.clientdata.location.UserLocation;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdditionalAuthorizationEmailVerificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final int GENERATED_CODE = 123456;
    private static final String IP_ADDRESS = "192.168.0.1";
    private static final String BROWSER = "Chrome";
    private static final String LOCATION = "Warsaw, PL";

    @Mock
    private VerificationCodeEmailSender verificationCodeEmailSender;

    @Mock
    private SecretGenerator secretGenerator;

    @Mock
    private VerificationCodeVerifier verificationCodeVerifier;

    @Mock
    private VerificationCodeAttemptsTemplate attemptsTemplate;

    @Mock
    private SecuritySettingsRepository securitySettingsRepository;

    @Mock
    private VerificationCodeProperties properties;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OutboxService outboxService;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AdditionalAuthorizationEmailVerificationService service;

    private User user;

    private SecuritySettings securitySettings;

    private MockedStatic<ClientIp> clientIpMockedStatic;
    private MockedStatic<UserBrowser> userBrowserMockedStatic;
    private MockedStatic<UserLocation> userLocationMockedStatic;

    @BeforeEach
    void setUp() {
        service = new AdditionalAuthorizationEmailVerificationService(verificationCodeEmailSender, secretGenerator,
                verificationCodeVerifier, attemptsTemplate, securitySettingsRepository, passwordEncoder, properties,
                userManagerService, outboxService);
        user = mock(User.class);
        securitySettings = mock(SecuritySettings.class);
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(user.getSecuritySettings()).thenReturn(securitySettings);
    }

    private void mockClientDataUtils() {
        clientIpMockedStatic = mockStatic(ClientIp.class);
        userBrowserMockedStatic = mockStatic(UserBrowser.class);
        userLocationMockedStatic = mockStatic(UserLocation.class);

        clientIpMockedStatic.when(() -> ClientIp.getClientIpAddress(httpServletRequest)).thenReturn(IP_ADDRESS);
        userBrowserMockedStatic.when(() -> UserBrowser.getBrowser(httpServletRequest)).thenReturn(BROWSER);
        userLocationMockedStatic.when(() -> UserLocation.getLocationFromIp(IP_ADDRESS)).thenReturn(LOCATION);
    }

    @AfterEach
    void tearDown() {
        if (clientIpMockedStatic != null) {
            clientIpMockedStatic.close();
        }
        if (userBrowserMockedStatic != null) {
            userBrowserMockedStatic.close();
        }
        if (userLocationMockedStatic != null) {
            userLocationMockedStatic.close();
        }
    }

    @Nested
    class ConfirmAdditionalAuthorizationCode {

        @Test
        void shouldReturnAttemptsAndGeneratedAuthorizationCodeWhenVerificationSucceeds() {
            mockClientDataUtils();
            AdditionalAuthorizationEmailCodeRequest dto = new AdditionalAuthorizationEmailCodeRequest(GENERATED_CODE);
            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(2);
            when(securitySettingsRepository.getAdditionalAuthorizationAttemptsByUserId(USER_ID)).thenReturn(1);
            when(attemptsTemplate.getCurrent(any(), eq(1))).thenReturn(new AttemptsDto(1, 5, 4));

            AdditionalAuthorizationEmailCodeResponse result = service.confirmAdditionalAuthorizationCode(USER_ID, dto, httpServletRequest);

            assertEquals(1, result.attempts().used());
            assertEquals(5, result.attempts().max());
            assertEquals(4, result.attempts().remaining());
            verify(securitySettings).setAdditionalAuthorizationEmailCode(null);
            verify(securitySettings).setAdditionalAuthorizationEmailCodeExpiresAt(null);
            verify(securitySettings).setAdditionalAuthorizationAttempts(0);
            verify(securitySettings).setAdditionalAuthorizationEnabled(true);
            verify(securitySettings).setAdditionalAuthorizationCode(passwordEncoder.encode("123456"));
            verify(securitySettingsRepository, times(1)).save(securitySettings);
            verify(secretGenerator).generateAdditionalAuthorizationCode();
        }

        @Test
        void shouldPublishEnabledEventWhenVerificationSucceeds() {
            mockClientDataUtils();
            AdditionalAuthorizationEmailCodeRequest dto = new AdditionalAuthorizationEmailCodeRequest(GENERATED_CODE);
            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(2);
            when(securitySettingsRepository.getAdditionalAuthorizationAttemptsByUserId(USER_ID)).thenReturn(1);
            when(attemptsTemplate.getCurrent(any(), eq(1))).thenReturn(new AttemptsDto(1, 5, 4));

            service.confirmAdditionalAuthorizationCode(USER_ID, dto, httpServletRequest);

            ArgumentCaptor<AccountChangesActivityEvent> eventCaptor = ArgumentCaptor.forClass(AccountChangesActivityEvent.class);
            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()), eq("activity.account-changes"), eventCaptor.capture());

            AccountChangesActivityEvent publishedEvent = eventCaptor.getValue();
            assertEquals(AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_ENABLED, publishedEvent.type());
            assertEquals(USER_ID, publishedEvent.userId());
            assertEquals(IP_ADDRESS, publishedEvent.ipAddress());
            assertEquals(BROWSER, publishedEvent.browser());
            assertEquals(LOCATION, publishedEvent.location());
        }

        @Test
        void shouldPropagateExceptionWhenVerificationFails() {
            AdditionalAuthorizationEmailCodeRequest dto = new AdditionalAuthorizationEmailCodeRequest(GENERATED_CODE);
            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(2);
            doThrow(new RuntimeException("attempts exceeded"))
                    .when(verificationCodeVerifier)
                    .verifyAttemptsOrThrow(any(), any(), eq(GENERATED_CODE), any(), any());

            assertThrows(RuntimeException.class,
                    () -> service.confirmAdditionalAuthorizationCode(USER_ID, dto, httpServletRequest));

            verify(securitySettingsRepository, times(0)).save(any());
            verifyNoInteractions(outboxService);
        }
    }
}