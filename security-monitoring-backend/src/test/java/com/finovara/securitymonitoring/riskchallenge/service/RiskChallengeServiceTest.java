// RiskChallengeServiceTest.java
package com.finovara.securitymonitoring.riskchallenge.service;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.securitymonitoring.exception.badrequest.InvalidChallengeException;
import com.finovara.securitymonitoring.exception.conflict.RiskOperationStateException;
import com.finovara.securitymonitoring.feignclient.AuthBackendClient;
import com.finovara.securitymonitoring.riskchallenge.dto.ChallengeConfirmationResponse;
import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import com.finovara.securitymonitoring.util.email.EmailTemplateService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskChallengeServiceTest {

    @Mock
    private RiskOperationRepository riskOperationRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private RiskChallengeService riskChallengeService;

    private static final Long RISK_OPERATION_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String RECIPIENT_EMAIL = "user@test.com";
    private static final int EXPIRES_MINUTES = 15;

    private RiskOperation operation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(riskChallengeService, "secureCodeExpiresMinutes", EXPIRES_MINUTES);
        operation = mock(RiskOperation.class);
    }

    @Nested
    class GenerateAndSendEmailCode {

        @Test
        void shouldSetEmailCodeOnOperationWhenGenerating() {
            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(operation).setEmailCode(anyString());
        }

        @Test
        void shouldSetSixDigitEmailCodeWhenGenerating() {
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(operation).setEmailCode(codeCaptor.capture());
            assertThat(codeCaptor.getValue()).matches("\\d{6}");
        }

        @Test
        void shouldSetEmailCodeExpiresAtBasedOnConfiguredMinutesWhenGenerating() {
            LocalDateTime before = LocalDateTime.now().plusMinutes(EXPIRES_MINUTES);

            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(operation).setEmailCodeExpiresAt(expiresCaptor.capture());
            LocalDateTime after = LocalDateTime.now().plusMinutes(EXPIRES_MINUTES);

            assertThat(expiresCaptor.getValue()).isBetween(before.minusSeconds(5), after.plusSeconds(5));
        }

        @Test
        void shouldSaveOperationWhenGenerating() {
            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(riskOperationRepository).save(operation);
        }
    }

    @Nested
    class ConfirmPassword {

        private static final String PASSWORD = "secret123";

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findById(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            when(operation.getUserId()).thenReturn(USER_ID);
        }

        @Test
        void shouldConfirmPasswordSuccessfullyWhenRequiredAndValid() {
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(true);

            ChallengeConfirmationResponse response = riskChallengeService.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            assertThat(response.riskOperationId()).isEqualTo(RISK_OPERATION_ID);
            assertThat(response.fullyVerified()).isTrue();
        }

        @Test
        void shouldVerifyPasswordWithAuthBackendWhenConfirming() {
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(false);

            riskChallengeService.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            verify(authBackendClient).verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));
        }

        @Test
        void shouldSetPasswordConfirmedAndSaveWhenValid() {
            when(operation.requiresPassword()).thenReturn(true);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.isFullyVerified()).thenReturn(false);

            riskChallengeService.confirmPassword(RISK_OPERATION_ID, PASSWORD);

            verify(operation).setPasswordConfirmed(true);
            verify(riskOperationRepository).save(operation);
        }

        @Test
        void shouldThrowInvalidChallengeExceptionWhenAuthBackendReturnsUnauthorized() {
            when(operation.requiresPassword()).thenReturn(true);
            doThrow(mock(FeignException.Unauthorized.class))
                    .when(authBackendClient).verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

            assertThrows(InvalidChallengeException.class, () -> riskChallengeService.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verify(operation, never()).setPasswordConfirmed(any(Boolean.class));
            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidChallengeExceptionWhenAuthBackendReturnsForbidden() {
            when(operation.requiresPassword()).thenReturn(true);
            doThrow(mock(FeignException.Forbidden.class))
                    .when(authBackendClient).verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

            assertThrows(InvalidChallengeException.class, () -> riskChallengeService.confirmPassword(RISK_OPERATION_ID, PASSWORD));

            verify(operation, never()).setPasswordConfirmed(any(Boolean.class));
            verify(riskOperationRepository, never()).save(any());
        }

    }

    @Nested
    class ConfirmEmailCode {

        private static final String CODE = "123456";

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findById(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
        }

        @Test
        void shouldConfirmEmailCodeSuccessfullyWhenRequiredAndValid() {
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(operation.isFullyVerified()).thenReturn(true);

            ChallengeConfirmationResponse response = riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

            assertThat(response.riskOperationId()).isEqualTo(RISK_OPERATION_ID);
            assertThat(response.fullyVerified()).isTrue();
        }

        @Test
        void shouldSetEmailCodeConfirmedAndSaveWhenValid() {
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(CODE);
            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
            when(operation.getUserId()).thenReturn(USER_ID);
            when(operation.isFullyVerified()).thenReturn(false);

            riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

            verify(operation).setEmailCodeConfirmed(true);
            verify(riskOperationRepository).save(operation);
        }

        @Test
        void shouldThrowRiskOperationStateExceptionWhenEmailCodeNotRequired() {
            when(operation.requiresEmailCode()).thenReturn(false);

            assertThrows(RiskOperationStateException.class, () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(any(Boolean.class));
            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidChallengeExceptionWhenExpiresAtIsNull() {
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(null);

            assertThrows(InvalidChallengeException.class, () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(any(Boolean.class));
            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidChallengeExceptionWhenCodeHasExpired() {
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

            assertThrows(InvalidChallengeException.class, () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(any(Boolean.class));
            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidChallengeExceptionWhenStoredEmailCodeIsNull() {
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn(null);

            assertThrows(InvalidChallengeException.class, () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(any(Boolean.class));
            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidChallengeExceptionWhenCodeDoesNotMatch() {
            when(operation.requiresEmailCode()).thenReturn(true);
            when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
            when(operation.getEmailCode()).thenReturn("999999");

            assertThrows(InvalidChallengeException.class, () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(operation, never()).setEmailCodeConfirmed(any(Boolean.class));
            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenOperationNotFound() {
            when(riskOperationRepository.findById(RISK_OPERATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));
        }
    }
}