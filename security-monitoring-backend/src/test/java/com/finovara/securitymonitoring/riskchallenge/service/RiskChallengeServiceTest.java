package com.finovara.securitymonitoring.riskchallenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RiskChallengeServiceTest {

    private static final Long RISK_OPERATION_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;
    private static final String RECIPIENT_EMAIL = "user@test.com";
    private static final String EMAIL_SUBJECT = "Kod autoryzacyjny Finovara";
    private static final String EMAIL_TEMPLATE = "/email/security-authorization-code.html";
    private static final String EXPIRES_FIELD = "secureCodeExpiresMinutes";
    private static final int EXPIRES_MINUTES = 15;

    @Mock
    private RiskOperationRepository riskOperationRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private RiskOperation operation;

    @InjectMocks
    private RiskChallengeService riskChallengeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(riskChallengeService, EXPIRES_FIELD, EXPIRES_MINUTES);
    }

    @Nested
    class GenerateAndSendEmailCode {

        @Test
        void shouldSetSixDigitEmailCodeOnOperationWhenGenerating() {
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(operation).setEmailCode(codeCaptor.capture());
            assertThat(codeCaptor.getValue()).matches("\\d{6}");
        }

        @Test
        void shouldSetEmailCodeExpiresAtBasedOnConfiguredMinutesWhenGenerating() {
            ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            LocalDateTime before = LocalDateTime.now().plusMinutes(EXPIRES_MINUTES);

            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            LocalDateTime after = LocalDateTime.now().plusMinutes(EXPIRES_MINUTES);
            verify(operation).setEmailCodeExpiresAt(expiresCaptor.capture());
            assertThat(expiresCaptor.getValue()).isBetween(before, after);
        }

        @Test
        void shouldSetEmailCodeExpiresAtBasedOnChangedConfigurationWhenGenerating() {
            int changedMinutes = 30;
            ReflectionTestUtils.setField(riskChallengeService, EXPIRES_FIELD, changedMinutes);
            ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            LocalDateTime before = LocalDateTime.now().plusMinutes(changedMinutes);

            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            LocalDateTime after = LocalDateTime.now().plusMinutes(changedMinutes);
            verify(operation).setEmailCodeExpiresAt(expiresCaptor.capture());
            assertThat(expiresCaptor.getValue()).isBetween(before, after);
        }

        @Test
        void shouldSaveOperationWhenGenerating() {
            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(riskOperationRepository).save(operation);
        }

        @Test
        void shouldSendEmailWithSameCodeThatWasSetOnOperationWhenGenerating() {
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            verify(operation).setEmailCode(codeCaptor.capture());
            verify(emailTemplateService).sendEmail(RECIPIENT_EMAIL, EMAIL_SUBJECT, EMAIL_TEMPLATE,
                    Map.of("code", codeCaptor.getValue()));
        }

        @Test
        void shouldSendEmailWithNullRecipientWhenRecipientIsNull() {
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            riskChallengeService.generateAndSendEmailCode(operation, null);

            verify(operation).setEmailCode(codeCaptor.capture());
            verify(emailTemplateService).sendEmail(null, EMAIL_SUBJECT, EMAIL_TEMPLATE,
                    Map.of("code", codeCaptor.getValue()));
        }

        @Test
        void shouldPrepareAndSaveOperationBeforeSendingEmailWhenGenerating() {
            riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL);

            InOrder inOrder = inOrder(operation, riskOperationRepository, emailTemplateService);
            inOrder.verify(operation).setEmailCode(anyString());
            inOrder.verify(operation).setEmailCodeExpiresAt(any(LocalDateTime.class));
            inOrder.verify(riskOperationRepository).save(operation);
            inOrder.verify(emailTemplateService)
                    .sendEmail(eq(RECIPIENT_EMAIL), eq(EMAIL_SUBJECT), eq(EMAIL_TEMPLATE), any());
        }

        @Test
        void shouldThrowExceptionWhenOperationIsNull() {
            assertThrows(NullPointerException.class,
                    () -> riskChallengeService.generateAndSendEmailCode(null, RECIPIENT_EMAIL));

            verifyNoInteractions(riskOperationRepository, emailTemplateService);
        }

        @Test
        void shouldThrowExceptionWhenSavingOperationFails() {
            when(riskOperationRepository.save(operation)).thenThrow(new IllegalStateException("save failed"));

            assertThrows(IllegalStateException.class,
                    () -> riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL));

            verifyNoInteractions(emailTemplateService);
        }

        @Test
        void shouldThrowExceptionWhenSendingEmailFails() {
            doThrow(new IllegalStateException("send failed"))
                    .when(emailTemplateService)
                    .sendEmail(eq(RECIPIENT_EMAIL), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class,
                    () -> riskChallengeService.generateAndSendEmailCode(operation, RECIPIENT_EMAIL));

            verify(riskOperationRepository).save(operation);
        }
    }

    @Nested
    class ConfirmPassword {

        private static final String PASSWORD = "secret123";

        @Test
        void shouldThrowExceptionWhenOperationNotFound() {
            when(riskOperationRepository.findByIdAndUserId(RISK_OPERATION_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD));

            verifyNoInteractions(authBackendClient);
            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenOperationNotFoundForNullUserId() {
            when(riskOperationRepository.findByIdAndUserId(RISK_OPERATION_ID, null))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> riskChallengeService.confirmPassword(null, RISK_OPERATION_ID, PASSWORD));

            verifyNoInteractions(authBackendClient);
        }

        @Test
        void shouldThrowExceptionWhenOperationNotFoundForNullRiskOperationId() {
            when(riskOperationRepository.findByIdAndUserId(null, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> riskChallengeService.confirmPassword(USER_ID, null, PASSWORD));

            verifyNoInteractions(authBackendClient);
        }

        @Nested
        class WhenOperationExists {

            @BeforeEach
            void setUp() {
                when(riskOperationRepository.findByIdAndUserId(RISK_OPERATION_ID, USER_ID))
                        .thenReturn(Optional.of(operation));
            }

            @Test
            void shouldThrowExceptionWhenOperationDoesNotRequirePassword() {
                when(operation.requiresPassword()).thenReturn(false);

                assertThrows(RiskOperationStateException.class,
                        () -> riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD));

                verifyNoInteractions(authBackendClient);
                verify(operation, never()).setPasswordConfirmed(anyBoolean());
                verify(riskOperationRepository, never()).save(any());
            }

            @Nested
            class WhenPasswordIsRequired {

                @BeforeEach
                void setUp() {
                    when(operation.requiresPassword()).thenReturn(true);
                    when(operation.getUserId()).thenReturn(USER_ID);
                }

                @Test
                void shouldReturnFullyVerifiedResponseWhenPasswordIsValid() {
                    when(operation.getId()).thenReturn(RISK_OPERATION_ID);
                    when(operation.isFullyVerified()).thenReturn(true);

                    ChallengeConfirmationResponse response =
                            riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD);

                    assertThat(response).isEqualTo(new ChallengeConfirmationResponse(RISK_OPERATION_ID, true));
                }

                @Test
                void shouldReturnNotFullyVerifiedResponseWhenOtherChallengesArePending() {
                    when(operation.getId()).thenReturn(RISK_OPERATION_ID);
                    when(operation.isFullyVerified()).thenReturn(false);

                    ChallengeConfirmationResponse response =
                            riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD);

                    assertThat(response).isEqualTo(new ChallengeConfirmationResponse(RISK_OPERATION_ID, false));
                }

                @Test
                void shouldVerifyPasswordWithAuthBackendWhenConfirming() {
                    ArgumentCaptor<ConfirmPasswordDto> dtoCaptor = ArgumentCaptor.forClass(ConfirmPasswordDto.class);

                    riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD);

                    verify(authBackendClient).verifyPassword(eq(USER_ID), dtoCaptor.capture());
                    assertThat(dtoCaptor.getValue()).usingRecursiveComparison()
                            .isEqualTo(new ConfirmPasswordDto(PASSWORD));
                }

                @Test
                void shouldVerifyPasswordUsingUserIdFromOperationWhenOperationUserIdDiffers() {
                    when(operation.getUserId()).thenReturn(OTHER_USER_ID);

                    riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD);

                    verify(authBackendClient).verifyPassword(eq(OTHER_USER_ID), any(ConfirmPasswordDto.class));
                }

                @Test
                void shouldVerifyPasswordWithNullPasswordWhenPasswordIsNull() {
                    ArgumentCaptor<ConfirmPasswordDto> dtoCaptor = ArgumentCaptor.forClass(ConfirmPasswordDto.class);

                    riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, null);

                    verify(authBackendClient).verifyPassword(eq(USER_ID), dtoCaptor.capture());
                    assertThat(dtoCaptor.getValue()).usingRecursiveComparison()
                            .isEqualTo(new ConfirmPasswordDto(null));
                }

                @Test
                void shouldSetPasswordConfirmedAndSaveOperationWhenPasswordIsValid() {
                    riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD);

                    verify(operation).setPasswordConfirmed(true);
                    verify(riskOperationRepository).save(operation);
                }

                @Test
                void shouldVerifyPasswordBeforeMarkingConfirmedAndSavingWhenPasswordIsValid() {
                    riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD);

                    InOrder inOrder = inOrder(authBackendClient, operation, riskOperationRepository);
                    inOrder.verify(authBackendClient).verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));
                    inOrder.verify(operation).setPasswordConfirmed(true);
                    inOrder.verify(riskOperationRepository).save(operation);
                }

                @Test
                void shouldNotSetEmailCodeConfirmedWhenConfirmingPassword() {
                    riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD);

                    verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                }

                @Test
                void shouldThrowExceptionWhenAuthBackendReturnsUnauthorized() {
                    doThrow(mock(FeignException.Unauthorized.class))
                            .when(authBackendClient).verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

                    assertThrows(InvalidChallengeException.class,
                            () -> riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD));

                    verify(operation, never()).setPasswordConfirmed(anyBoolean());
                    verify(riskOperationRepository, never()).save(any());
                }

                @Test
                void shouldThrowExceptionWhenAuthBackendReturnsForbidden() {
                    doThrow(mock(FeignException.Forbidden.class))
                            .when(authBackendClient).verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

                    assertThrows(InvalidChallengeException.class,
                            () -> riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD));

                    verify(operation, never()).setPasswordConfirmed(anyBoolean());
                    verify(riskOperationRepository, never()).save(any());
                }

                @Test
                void shouldThrowExceptionWhenAuthBackendReturnsServerError() {
                    doThrow(mock(FeignException.InternalServerError.class))
                            .when(authBackendClient).verifyPassword(eq(USER_ID), any(ConfirmPasswordDto.class));

                    assertThrows(FeignException.InternalServerError.class,
                            () -> riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD));

                    verify(operation, never()).setPasswordConfirmed(anyBoolean());
                    verify(riskOperationRepository, never()).save(any());
                }

                @Test
                void shouldThrowExceptionWhenSavingOperationFails() {
                    when(riskOperationRepository.save(operation)).thenThrow(new IllegalStateException("save failed"));

                    assertThrows(IllegalStateException.class,
                            () -> riskChallengeService.confirmPassword(USER_ID, RISK_OPERATION_ID, PASSWORD));
                }
            }
        }
    }

    @Nested
    class ConfirmEmailCode {

        private static final String CODE = "123456";
        private static final String OTHER_CODE = "999999";

        @Test
        void shouldThrowExceptionWhenOperationNotFound() {
            when(riskOperationRepository.findById(RISK_OPERATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

            verify(riskOperationRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenOperationNotFoundForNullRiskOperationId() {
            when(riskOperationRepository.findById(null)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> riskChallengeService.confirmEmailCode(null, CODE));

            verify(riskOperationRepository, never()).save(any());
        }

        @Nested
        class WhenOperationExists {

            @BeforeEach
            void setUp() {
                when(riskOperationRepository.findById(RISK_OPERATION_ID)).thenReturn(Optional.of(operation));
            }

            @Test
            void shouldThrowExceptionWhenEmailCodeIsNotRequired() {
                when(operation.requiresEmailCode()).thenReturn(false);

                assertThrows(RiskOperationStateException.class,
                        () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

                verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                verify(riskOperationRepository, never()).save(any());
            }

            @Nested
            class WhenEmailCodeIsRequired {

                @BeforeEach
                void setUp() {
                    when(operation.requiresEmailCode()).thenReturn(true);
                }

                @Test
                void shouldThrowExceptionWhenExpirationDateIsNull() {
                    when(operation.getEmailCodeExpiresAt()).thenReturn(null);

                    assertThrows(InvalidChallengeException.class,
                            () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

                    verify(operation, never()).getEmailCode();
                    verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                    verify(riskOperationRepository, never()).save(any());
                }

                @Test
                void shouldThrowExceptionWhenCodeHasExpired() {
                    when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

                    assertThrows(InvalidChallengeException.class,
                            () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

                    verify(operation, never()).getEmailCode();
                    verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                    verify(riskOperationRepository, never()).save(any());
                }

                @Nested
                class WhenCodeIsNotExpired {

                    @BeforeEach
                    void setUp() {
                        when(operation.getEmailCodeExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
                    }

                    @Test
                    void shouldThrowExceptionWhenStoredEmailCodeIsNull() {
                        when(operation.getEmailCode()).thenReturn(null);

                        assertThrows(InvalidChallengeException.class,
                                () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

                        verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                        verify(riskOperationRepository, never()).save(any());
                    }

                    @Test
                    void shouldThrowExceptionWhenCodeDoesNotMatch() {
                        when(operation.getEmailCode()).thenReturn(OTHER_CODE);

                        assertThrows(InvalidChallengeException.class,
                                () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));

                        verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                        verify(riskOperationRepository, never()).save(any());
                    }

                    @Test
                    void shouldThrowExceptionWhenProvidedCodeIsNull() {
                        when(operation.getEmailCode()).thenReturn(CODE);

                        assertThrows(InvalidChallengeException.class,
                                () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, null));

                        verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                        verify(riskOperationRepository, never()).save(any());
                    }

                    @Test
                    void shouldThrowExceptionWhenProvidedCodeContainsWhitespace() {
                        when(operation.getEmailCode()).thenReturn(CODE);

                        assertThrows(InvalidChallengeException.class,
                                () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, " " + CODE));

                        verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                        verify(riskOperationRepository, never()).save(any());
                    }

                    @Test
                    void shouldThrowExceptionWhenProvidedCodeIsEmpty() {
                        when(operation.getEmailCode()).thenReturn(CODE);

                        assertThrows(InvalidChallengeException.class,
                                () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, ""));

                        verify(operation, never()).setEmailCodeConfirmed(anyBoolean());
                        verify(riskOperationRepository, never()).save(any());
                    }

                    @Nested
                    class WhenCodeMatches {

                        @BeforeEach
                        void setUp() {
                            when(operation.getEmailCode()).thenReturn(CODE);
                        }

                        @Test
                        void shouldReturnFullyVerifiedResponseWhenCodeIsValid() {
                            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
                            when(operation.isFullyVerified()).thenReturn(true);

                            ChallengeConfirmationResponse response =
                                    riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

                            assertThat(response).isEqualTo(new ChallengeConfirmationResponse(RISK_OPERATION_ID, true));
                        }

                        @Test
                        void shouldReturnNotFullyVerifiedResponseWhenOtherChallengesArePending() {
                            when(operation.getId()).thenReturn(RISK_OPERATION_ID);
                            when(operation.isFullyVerified()).thenReturn(false);

                            ChallengeConfirmationResponse response =
                                    riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

                            assertThat(response).isEqualTo(new ChallengeConfirmationResponse(RISK_OPERATION_ID, false));
                        }

                        @Test
                        void shouldSetEmailCodeConfirmedAndSaveOperationWhenCodeIsValid() {
                            riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

                            verify(operation).setEmailCodeConfirmed(true);
                            verify(riskOperationRepository).save(operation);
                        }

                        @Test
                        void shouldMarkCodeConfirmedBeforeSavingWhenCodeIsValid() {
                            riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

                            InOrder inOrder = inOrder(operation, riskOperationRepository);
                            inOrder.verify(operation).setEmailCodeConfirmed(true);
                            inOrder.verify(riskOperationRepository).save(operation);
                        }

                        @Test
                        void shouldNotSetPasswordConfirmedWhenConfirmingEmailCode() {
                            riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

                            verify(operation, never()).setPasswordConfirmed(anyBoolean());
                        }

                        @Test
                        void shouldNotCallAuthBackendWhenConfirmingEmailCode() {
                            riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE);

                            verifyNoInteractions(authBackendClient);
                        }

                        @Test
                        void shouldThrowExceptionWhenSavingOperationFails() {
                            when(riskOperationRepository.save(operation)).thenThrow(new IllegalStateException("save failed"));

                            assertThrows(IllegalStateException.class,
                                    () -> riskChallengeService.confirmEmailCode(RISK_OPERATION_ID, CODE));
                        }
                    }
                }
            }
        }
    }
}