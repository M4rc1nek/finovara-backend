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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
    private static final String EMAIL = "user@test.com";
    private static final int GENERATED_CODE = 123456;

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

    private AdditionalAuthorizationEmailVerificationService service;

    private User user;

    private SecuritySettings securitySettings;

    @BeforeEach
    void setUp() {
        service = new AdditionalAuthorizationEmailVerificationService(verificationCodeEmailSender, secretGenerator,
                verificationCodeVerifier, attemptsTemplate, securitySettingsRepository, passwordEncoder,properties, userManagerService);
        user = mock(User.class);
        securitySettings = mock(SecuritySettings.class);
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(user.getSecuritySettings()).thenReturn(securitySettings);
    }

    @Nested
    class ConfirmAdditionalAuthorizationCode {

        @Test
        void shouldReturnAttemptsAndGeneratedAuthorizationCodeWhenVerificationSucceeds() {
            AdditionalAuthorizationEmailCodeRequest dto = new AdditionalAuthorizationEmailCodeRequest(GENERATED_CODE);
            when(properties.getMaxAttempts()).thenReturn(5);
            when(properties.getAttemptsExpirationMinutes()).thenReturn(2);
            when(securitySettingsRepository.getAdditionalAuthorizationAttemptsByUserId(USER_ID)).thenReturn(1);
            when(attemptsTemplate.getCurrent(any(), eq(1))).thenReturn(new AttemptsDto(1, 5, 4));

            AdditionalAuthorizationEmailCodeResponse result = service.confirmAdditionalAuthorizationCode(USER_ID, dto);

            assertEquals(1, result.attempts().used());
            assertEquals(5, result.attempts().max());
            assertEquals(4, result.attempts().remaining());
            verify(securitySettings).setAdditionalAuthorizationEmailCode(null);
            verify(securitySettings).setAdditionalAuthorizationEmailCodeExpiresAt(null);
            verify(securitySettings).setAdditionalAuthorizationAttempts(0);
            verify(securitySettings).setAdditionalAuthorizationCode(passwordEncoder.encode("123456"));
            verify(securitySettingsRepository, times(1)).save(securitySettings);
            verify(secretGenerator).generateAdditionalAuthorizationCode();
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
                    () -> service.confirmAdditionalAuthorizationCode(USER_ID, dto));

            verify(securitySettingsRepository, times(0)).save(any());
        }
    }
}