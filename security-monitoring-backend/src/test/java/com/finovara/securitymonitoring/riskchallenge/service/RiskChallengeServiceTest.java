package com.finovara.securitymonitoring.riskchallenge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.securitymonitoring.exception.badrequest.InvalidChallengeException;
import com.finovara.securitymonitoring.exception.conflict.RiskOperationStateException;
import com.finovara.securitymonitoring.feignclient.AuthBackendClient;
import com.finovara.securitymonitoring.riskchallenge.dto.ChallengeConfirmationResponse;
import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import com.finovara.securitymonitoring.util.email.EmailTemplateService;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskChallengeServiceTest {

    private static final String EMAIL_SUBJECT = "Kod autoryzacyjny Finovara";
    private static final String EMAIL_TEMPLATE = "/email/security-authorization-code.html";
    private static final String CODE_REGEX = "\\d{6}";
    private static final int EXPIRES_MINUTES = 10;
    private static final Long RISK_OPERATION_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String RECIPIENT_EMAIL = "user@finovara.com";
    private static final String PASSWORD = "secret";
    private static final String CODE = "123456";
    private static final String WRONG_CODE = "654321";

    @Mock
    private RiskOperationRepository riskOperationRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private RiskOperation operation;

    @Mock
    private FeignException.Unauthorized unauthorizedException;

    @Mock
    private FeignException.Forbidden forbiddenException;

    @Mock
    private FeignException.InternalServerError serverErrorException;

    @Captor
    private ArgumentCaptor<String> codeCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> expiresAtCaptor;

    @Captor
    private ArgumentCaptor<ConfirmPasswordDto> passwordDtoCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    @InjectMocks
    private RiskChallengeService service;

    @BeforeEach
    void setUp() {
        setField(service, "secureCodeExpiresMinutes", EXPIRES_MINUTES);
    }

    @Nested
    class GenerateAndSendEmailCode {

        @Test
        void shouldSetSixDigitCodeOnOperationWhenGeneratingCode() {
            service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(operation).setEmailCode(codeCaptor.capture());
            assertTrue(codeCaptor.getValue().matches(CODE_REGEX));
        }

        @Test
        void shouldSetExpirationBasedOnConfiguredMinutesWhenGeneratingCode() {
            LocalDateTime before = LocalDateTime.now().plusMinutes(EXPIRES_MINUTES);

            service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            LocalDateTime after = LocalDateTime.now().plusMinutes(EXPIRES_MINUTES);
            verify(operation).setEmailCodeExpiresAt(expiresAtCaptor.capture());
            LocalDateTime expiresAt = expiresAtCaptor.getValue();
            assertFalse(expiresAt.isBefore(before));
            assertFalse(expiresAt.isAfter(after));
        }

        @Test
        void shouldSaveOperationWhenGeneratingCode() {
            service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(riskOperationRepository, times(1)).save(operation);
            verifyNoMoreInteractions(riskOperationRepository);
        }


        @Test
        void shouldSaveOperationBeforeSendingEmailWhenGeneratingCode() {
            service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            InOrder inOrder = inOrder(riskOperationRepository, emailTemplateService);
            inOrder.verify(riskOperationRepository).save(operation);
            inOrder.verify(emailTemplateService).sendEmail(anyString(), anyString(), anyString(), anyMap());
        }

        @Test
        void shouldNotUseAuthBackendWhenGeneratingCode() {
            service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verifyNoInteractions(authBackendClient);
        }

        @Test
        void shouldGenerateCodeWithLeadingZerosPreservedWhenGeneratingManyCodes() {
            for (int i = 0; i < 200; i++) {
                service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);
            }

            verify(operation, times(200)).setEmailCode(codeCaptor.capture());
            for (String generated : codeCaptor.getAllValues()) {
                assertEquals(6, generated.length());
                assertTrue(generated.matches(CODE_REGEX));
            }
        }

        @Test
        void shouldThrowExceptionWhenOperationIsNull() {
            assertThrows(NullPointerException.class, () -> service.generateAndSendEmailCode(null, RECIPIENT_EMAIL));

            verifyNoInteractions(riskOperationRepository, emailTemplateService);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            when(riskOperationRepository.save(operation)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL));

            verifyNoInteractions(emailTemplateService);
        }

        @Test
        void shouldThrowExceptionWhenEmailSendingFails() {
            doThrow(new IllegalStateException()).when(emailTemplateService)
                    .sendEmail(anyString(), anyString(), anyString(), anyMap());

            assertThrows(IllegalStateException.class,
                    () -> service.generateAndSendEmailCode(operation, RECIPIENT_EMAIL));
        }
    }

    @Nested
    class ConfirmPassword {

        @Test
        void shouldConfirmPasswordAndReturnResponseWhenPasswordIsCorrect() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(true);

            ChallengeConfirmationResponse result = service.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            assertNotNull(result);
            verify(operation).setPasswordConfirmed(true);
            verify(riskOperationRepository).save(operation);
        }

        @Test
        void shouldReturnNotFullyVerifiedResponseWhenOtherChallengesArePending() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(false);

            ChallengeConfirmationResponse result = service.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            assertEquals(new ChallengeConfirmationResponse(RISK_OPERATION_ID, false), result);
        }

        @Test
        void shouldReturnFullyVerifiedResponseWhenAllChallengesAreCompleted() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(true);

            ChallengeConfirmationResponse result = service.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            assertEquals(new ChallengeConfirmationResponse(RISK_OPERATION_ID, true), result);
        }

        @Test
        void shouldVerifyPasswordWithOperationUserIdWhenConfirmingPassword() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);

            service.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            verify(authBackendClient).verifyPassword(eq(USER_ID), passwordDtoCaptor.capture());
            assertEquals(new ConfirmPasswordDto(PASSWORD), passwordDtoCaptor.getValue());
        }

        @Test
        void shouldLockOperationForUpdateWhenConfirmingPassword() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);

            service.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            verify(riskOperationRepository, times(1)).findByIdForUpdate(RISK_OPERATION_ID);
        }

        @Test
        void shouldThrowExceptionWhenOperationIsNotFound() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verifyNoInteractions(authBackendClient);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenOperationDoesNotRequirePassword() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(false);

            assertThrows(RiskOperationStateException.class,
                    () -> service.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verifyNoInteractions(authBackendClient);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenPasswordIsUnauthorized() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);
            doThrow(unauthorizedException).when(authBackendClient)
                    .verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

            assertThrows(InvalidChallengeException.class,
                    () -> service.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verify(operation, never()).setPasswordConfirmed(true);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenPasswordVerificationIsForbidden() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);
            doThrow(forbiddenException).when(authBackendClient)
                    .verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

            assertThrows(InvalidChallengeException.class,
                    () -> service.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verify(operation, never()).setPasswordConfirmed(true);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldPropagateExceptionWhenAuthBackendFailsWithServerError() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);
            doThrow(serverErrorException).when(authBackendClient)
                    .verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

            assertThrows(FeignException.InternalServerError.class,
                    () -> service.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(riskOperationRepository.save(operation)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.confirmPassword(RISK_OPERATION_ID, PASSWORD));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLookupFails() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verifyNoInteractions(authBackendClient);
        }
    }

    @Nested
    class ConfirmEmailCode {

        @Test
        void shouldConfirmEmailCodeAndReturnResponseWhenCodeIsCorrect() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(true);

            ChallengeConfirmationResponse result = service.confirmEmailCode(RISK_OPERATION_ID, CODE);

            assertEquals(new ChallengeConfirmationResponse(RISK_OPERATION_ID, true), result);
            verify(operation).setEmailCodeConfirmed(true);
            verify(riskOperationRepository).save(operation);
        }

        @Test
        void shouldReturnNotFullyVerifiedResponseWhenOtherChallengesArePending() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(false);

            ChallengeConfirmationResponse result = service.confirmEmailCode(RISK_OPERATION_ID, CODE);

            assertEquals(new ChallengeConfirmationResponse(RISK_OPERATION_ID, false), result);
        }

        @Test
        void shouldNotUseAuthBackendWhenConfirmingEmailCode() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);
            when(operation.getUserId()).thenReturn(USER_ID);

            service.confirmEmailCode(RISK_OPERATION_ID, CODE);

            verifyNoInteractions(authBackendClient, emailTemplateService);
        }

        @Test
        void shouldThrowExceptionWhenOperationIsNotFound() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenOperationDoesNotRequireEmailCode() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(false);

            assertThrows(RiskOperationStateException.class,
                    () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenExpirationIsNull() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(null);

            assertThrows(InvalidChallengeException.class, () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(true);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenCodeIsExpired() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

            assertThrows(InvalidChallengeException.class, () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(true);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenCodeIsExpiredEvenIfCodeMatches() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().minusSeconds(1));

            assertThrows(InvalidChallengeException.class, () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).getEmailCode();
        }

        @Test
        void shouldThrowExceptionWhenStoredCodeIsNull() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(null);

            assertThrows(InvalidChallengeException.class, () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(true);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenCodeDoesNotMatch() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);

            assertThrows(InvalidChallengeException.class,
                    () -> service.confirmEmailCode(RISK_OPERATION_ID, WRONG_CODE));

            verify(operation, never()).setEmailCodeConfirmed(true);
            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenProvidedCodeIsNull() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);

            assertThrows(InvalidChallengeException.class, () -> service.confirmEmailCode(RISK_OPERATION_ID, null));

            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);
            when(riskOperationRepository.save(operation)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLookupFails() {
            when(riskOperationRepository.findByIdForUpdate(RISK_OPERATION_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.confirmEmailCode(RISK_OPERATION_ID, CODE));
        }
    }
}