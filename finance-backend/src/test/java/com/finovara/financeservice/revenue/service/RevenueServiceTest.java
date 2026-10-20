package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.revenue.RevenueManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long REVENUE_ID = 10L;
    private static final BigDecimal AMOUNT = new BigDecimal("100");
    private static final BigDecimal OLD_AMOUNT = new BigDecimal("50");
    private static final RevenueCategory CATEGORY = RevenueCategory.INVESTMENT;
    private static final RevenueCategory OLD_CATEGORY = RevenueCategory.SALARY;
    private static final String DESCRIPTION = "new description";
    private static final String OLD_DESCRIPTION = "old description";
    private static final String AUTHORIZATION_CODE = "111111";
    private static final String USER_EMAIL = "user@test.com";
    private static final String RISK_SOURCE_EVENT_ID = "risk-source-event-id";
    private static final String AGGREGATE = "Revenue";
    private static final String TOPIC = "revenue.created";

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private AutoPaymentsService autoPaymentsService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RevenueManagerService revenueManagerService;

    @Mock
    private RevenueMapper revenueMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private RiskGuardService riskGuardService;

    @Mock
    private HttpServletRequest servletRequest;

    @Captor
    private ArgumentCaptor<Revenue> revenueCaptor;

    @Captor
    private ArgumentCaptor<RevenueActivityEvent> eventCaptor;

    @InjectMocks
    private RevenueService revenueService;

    private RevenueDto revenueDto;

    private ConfirmAuthorizationCodeDto resolvedAuthorizationCode;

    @BeforeEach
    void setUp() {
        revenueDto = new RevenueDto(null, null, AMOUNT, CATEGORY, null, DESCRIPTION, AUTHORIZATION_CODE, RISK_SOURCE_EVENT_ID);
        resolvedAuthorizationCode = new ConfirmAuthorizationCodeDto(AUTHORIZATION_CODE);
    }

    private void stubRevenueSave() {
        when(revenueRepository.save(any(Revenue.class))).thenAnswer(invocation -> {
            Revenue revenue = invocation.getArgument(0);
            revenue.setId(REVENUE_ID);
            return revenue;
        });
    }

    private void verifyOutboxEvent(RevenueActivityEvent expected) {
        verify(outboxService).save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(LocalDateTime.class)
                .isEqualTo(expected);
    }

    @Nested
    class AddRevenue {

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThrows(NullPointerException.class,
                    () -> revenueService.addRevenue(null, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

            verifyNoInteractions(riskGuardService, authBackendClient, walletService, revenueRepository,
                    outboxService, autoPaymentsService);
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsNull() {
            RevenueDto dtoWithoutCategory =
                    new RevenueDto(null, null, AMOUNT, null, null, DESCRIPTION, AUTHORIZATION_CODE, RISK_SOURCE_EVENT_ID);

            assertThrows(NullPointerException.class,
                    () -> revenueService.addRevenue(dtoWithoutCategory, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

            verifyNoInteractions(riskGuardService, walletService, revenueRepository, outboxService, autoPaymentsService);
        }

        @Test
        void shouldThrowExceptionWhenRiskGuardFails() {
            when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);
            doThrow(new IllegalStateException("risk detected")).when(riskGuardService)
                    .guard(USER_ID, RiskTriggerType.REVENUE, AMOUNT, CATEGORY.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);

            assertThrows(IllegalStateException.class,
                    () -> revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

            verify(authBackendClient, never()).confirmAuthorizationCode(any(), any());
            verifyNoInteractions(additionalAuthorizationCodeResolver, walletService, revenueRepository,
                    outboxService, autoPaymentsService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeCannotBeResolved() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE))
                    .thenThrow(new IllegalArgumentException("invalid code"));

            assertThrows(IllegalArgumentException.class,
                    () -> revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

            verify(authBackendClient, never()).confirmAuthorizationCode(any(), any());
            verifyNoInteractions(walletService, revenueRepository, outboxService, autoPaymentsService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            doThrow(new IllegalStateException("authorization denied")).when(authBackendClient)
                    .confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);

            assertThrows(IllegalStateException.class,
                    () -> revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

            verifyNoInteractions(walletService, revenueRepository, outboxService, autoPaymentsService);
        }

        @Nested
        class WhenVerificationPasses {

            @BeforeEach
            void setUp() {
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);
                when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            }

            @Test
            void shouldThrowExceptionWhenWalletUpdateFails() {
                doThrow(new IllegalStateException("wallet failed")).when(walletService).addBalanceToWallet(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class,
                        () -> revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

                verifyNoInteractions(revenueRepository, outboxService, autoPaymentsService);
            }

            @Test
            void shouldThrowExceptionWhenSavingRevenueFails() {
                when(revenueRepository.save(any(Revenue.class))).thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class,
                        () -> revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

                verifyNoInteractions(outboxService, autoPaymentsService);
            }

            @Nested
            class WhenRevenueIsSaved {

                @BeforeEach
                void setUp() {
                    stubRevenueSave();
                }

                @Test
                void shouldReturnRevenueIdWhenRevenueIsAdded() {
                    Long result = revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    assertThat(result).isEqualTo(REVENUE_ID);
                }

                @Test
                void shouldGuardAgainstRiskWhenOriginIsUserManual() {
                    revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(riskGuardService).guard(USER_ID, RiskTriggerType.REVENUE, AMOUNT, CATEGORY.name(),
                            USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
                }

                @Test
                void shouldConfirmAuthorizationCodeWhenOriginIsUserManual() {
                    revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
                    verify(authBackendClient).confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);
                }

                @Test
                void shouldAddAmountToWalletWhenRevenueIsAdded() {
                    revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
                }

                @Test
                void shouldSaveRevenueWithDtoDataWhenRevenueIsAdded() {
                    revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(revenueRepository).save(revenueCaptor.capture());
                    Revenue saved = revenueCaptor.getValue();
                    assertThat(saved.getAmount()).isEqualTo(AMOUNT);
                    assertThat(saved.getCategory()).isEqualTo(CATEGORY);
                    assertThat(saved.getDescription()).isEqualTo(DESCRIPTION);
                    assertThat(saved.getUserId()).isEqualTo(USER_ID);
                    assertThat(saved.getCreatedAt()).isEqualTo(LocalDate.now());
                }

                @Test
                void shouldSaveAddedRevenueEventWhenRevenueIsAdded() {
                    revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verifyOutboxEvent(new RevenueActivityEvent(USER_ID, RevenueActivityType.ADDED_REVENUE, AMOUNT, CATEGORY,
                            null, null, LocalDateTime.now()));
                }

                @Test
                void shouldApplyPiggyBankAutomationWhenRevenueIsAdded() {
                    revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
                }

                @Test
                void shouldProcessStepsInOrderWhenRevenueIsAdded() {
                    revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    InOrder inOrder = inOrder(riskGuardService, authBackendClient, walletService, revenueRepository,
                            outboxService, autoPaymentsService);
                    inOrder.verify(riskGuardService).guard(USER_ID, RiskTriggerType.REVENUE, AMOUNT, CATEGORY.name(),
                            USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
                    inOrder.verify(authBackendClient).confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);
                    inOrder.verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
                    inOrder.verify(revenueRepository).save(any(Revenue.class));
                    inOrder.verify(outboxService).save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), any());
                    inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
                }

                @Test
                void shouldThrowExceptionWhenOutboxServiceFails() {
                    doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                            .save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), any());

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

                    verifyNoInteractions(autoPaymentsService);
                }

                @Test
                void shouldThrowExceptionWhenPiggyBankAutomationFails() {
                    doThrow(new IllegalStateException("automation failed")).when(autoPaymentsService)
                            .handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                }
            }
        }

        @Nested
        class WhenVerificationIsSkipped {

            @BeforeEach
            void setUp() {
                stubRevenueSave();
            }

            @Test
            void shouldSkipVerificationWhenOriginIsRecurringSystem() {
                revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verifyNoInteractions(authBackendClient, additionalAuthorizationCodeResolver, riskGuardService);
            }

            @Test
            void shouldSkipVerificationWhenOriginIsNull() {
                revenueService.addRevenue(revenueDto, USER_ID, servletRequest, null);

                verifyNoInteractions(authBackendClient, additionalAuthorizationCodeResolver, riskGuardService);
            }

            @Test
            void shouldAddRevenueWhenOriginIsRecurringSystem() {
                Long result = revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                assertThat(result).isEqualTo(REVENUE_ID);
                verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
                verify(revenueRepository).save(any(Revenue.class));
                verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
            }

            @Test
            void shouldSaveAddedRevenueEventWhenOriginIsRecurringSystem() {
                revenueService.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verifyOutboxEvent(new RevenueActivityEvent(USER_ID, RevenueActivityType.ADDED_REVENUE, AMOUNT, CATEGORY,
                        null, null, LocalDateTime.now()));
            }
        }
    }

    @Nested
    class EditRevenue {

        private Revenue existingRevenue;

        @BeforeEach
        void setUp() {
            existingRevenue = new Revenue();
            existingRevenue.setId(REVENUE_ID);
            existingRevenue.setUserId(USER_ID);
            existingRevenue.setAmount(OLD_AMOUNT);
            existingRevenue.setCategory(OLD_CATEGORY);
            existingRevenue.setDescription(OLD_DESCRIPTION);
        }

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThrows(NullPointerException.class,
                    () -> revenueService.editRevenue(null, REVENUE_ID, USER_ID, servletRequest));

            verifyNoInteractions(authBackendClient, revenueManagerService, riskGuardService, walletService,
                    revenueRepository, outboxService, autoPaymentsService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeCannotBeResolved() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE))
                    .thenThrow(new IllegalArgumentException("invalid code"));

            assertThrows(IllegalArgumentException.class,
                    () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

            verifyNoInteractions(authBackendClient, revenueManagerService, riskGuardService, walletService,
                    revenueRepository, outboxService, autoPaymentsService);
        }

        @Nested
        class WhenAuthorizationIsConfirmed {

            @BeforeEach
            void setUp() {
                when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationConfirmationFails() {
                doThrow(new IllegalStateException("authorization denied")).when(authBackendClient)
                        .confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);

                assertThrows(IllegalStateException.class,
                        () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                verifyNoInteractions(revenueManagerService, riskGuardService, walletService, revenueRepository,
                        outboxService, autoPaymentsService);
            }

            @Test
            void shouldThrowExceptionWhenRevenueIsNotFoundForUser() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID))
                        .thenThrow(new RequestedEntityNotFoundException("Revenue not found"));

                assertThrows(RequestedEntityNotFoundException.class,
                        () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                verifyNoInteractions(riskGuardService, walletService, revenueRepository, outboxService, autoPaymentsService);
            }

            @Test
            void shouldThrowExceptionWhenRiskGuardFails() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existingRevenue);
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);
                doThrow(new IllegalStateException("risk detected")).when(riskGuardService)
                        .guard(USER_ID, RiskTriggerType.REVENUE, AMOUNT, CATEGORY.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);

                assertThrows(IllegalStateException.class,
                        () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                verifyNoInteractions(walletService, revenueRepository, outboxService, autoPaymentsService);
                assertThat(existingRevenue.getAmount()).isEqualTo(OLD_AMOUNT);
            }

            @Nested
            class WhenRiskGuardPasses {

                @BeforeEach
                void setUp() {
                    when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existingRevenue);
                    when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);
                }

                @Test
                void shouldReturnRevenueIdWhenRevenueIsEdited() {
                    Long result = revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    assertThat(result).isEqualTo(REVENUE_ID);
                }

                @Test
                void shouldConfirmAuthorizationCodeWhenRevenueIsEdited() {
                    revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    verify(authBackendClient).confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);
                }

                @Test
                void shouldGuardAgainstRiskWithNewDataWhenRevenueIsEdited() {
                    revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    verify(riskGuardService).guard(USER_ID, RiskTriggerType.REVENUE, AMOUNT, CATEGORY.name(),
                            USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
                }

                @Test
                void shouldRollbackOldAmountAndApplyNewAmountInPiggyBankWhenRevenueIsEdited() {
                    revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, OLD_AMOUNT, PiggyBankAutomationMode.ROLLBACK);
                    verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
                }

                @Test
                void shouldAddNewAmountAndRemoveOldAmountFromWalletWhenRevenueIsEdited() {
                    revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
                    verify(walletService).removeBalanceFromWallet(USER_ID, OLD_AMOUNT);
                }

                @Test
                void shouldUpdateRevenueFieldsAndSaveWhenRevenueIsEdited() {
                    revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    assertThat(existingRevenue.getAmount()).isEqualTo(AMOUNT);
                    assertThat(existingRevenue.getCategory()).isEqualTo(CATEGORY);
                    assertThat(existingRevenue.getDescription()).isEqualTo(DESCRIPTION);
                    verify(revenueRepository).save(existingRevenue);
                }

                @Test
                void shouldSaveEditedRevenueEventWithOldAndNewDataWhenRevenueIsEdited() {
                    revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    verifyOutboxEvent(new RevenueActivityEvent(USER_ID, RevenueActivityType.EDITED_REVENUE, AMOUNT, CATEGORY,
                            OLD_AMOUNT, OLD_CATEGORY, LocalDateTime.now()));
                }

                @Test
                void shouldProcessStepsInOrderWhenRevenueIsEdited() {
                    revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                    InOrder inOrder = inOrder(authBackendClient, revenueManagerService, riskGuardService,
                            autoPaymentsService, walletService, revenueRepository, outboxService);
                    inOrder.verify(authBackendClient).confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);
                    inOrder.verify(revenueManagerService).getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID);
                    inOrder.verify(riskGuardService).guard(USER_ID, RiskTriggerType.REVENUE, AMOUNT, CATEGORY.name(),
                            USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
                    inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, OLD_AMOUNT, PiggyBankAutomationMode.ROLLBACK);
                    inOrder.verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
                    inOrder.verify(walletService).removeBalanceFromWallet(USER_ID, OLD_AMOUNT);
                    inOrder.verify(revenueRepository).save(existingRevenue);
                    inOrder.verify(outboxService).save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), any());
                    inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
                }

                @Test
                void shouldThrowExceptionWhenPiggyBankRollbackFails() {
                    doThrow(new IllegalStateException("rollback failed")).when(autoPaymentsService)
                            .handleRevenuePiggyBankAutomation(USER_ID, OLD_AMOUNT, PiggyBankAutomationMode.ROLLBACK);

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                    verifyNoInteractions(walletService, revenueRepository, outboxService);
                    assertThat(existingRevenue.getAmount()).isEqualTo(OLD_AMOUNT);
                }

                @Test
                void shouldThrowExceptionWhenAddingBalanceToWalletFails() {
                    doThrow(new IllegalStateException("wallet failed")).when(walletService).addBalanceToWallet(USER_ID, AMOUNT);

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                    verify(walletService, never()).removeBalanceFromWallet(any(), any());
                    verifyNoInteractions(revenueRepository, outboxService);
                    assertThat(existingRevenue.getAmount()).isEqualTo(OLD_AMOUNT);
                }

                @Test
                void shouldThrowExceptionWhenRemovingBalanceFromWalletFails() {
                    doThrow(new IllegalStateException("wallet failed")).when(walletService).removeBalanceFromWallet(USER_ID, OLD_AMOUNT);

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                    verifyNoInteractions(revenueRepository, outboxService);
                }

                @Test
                void shouldThrowExceptionWhenSavingRevenueFails() {
                    when(revenueRepository.save(existingRevenue)).thenThrow(new IllegalStateException("db failed"));

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                    verifyNoInteractions(outboxService);
                }

                @Test
                void shouldThrowExceptionWhenOutboxServiceFails() {
                    doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                            .save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), any());

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));

                    verify(autoPaymentsService, never()).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
                }
            }
        }
    }

    @Nested
    class GetRevenue {

        private Revenue firstRevenue;

        private Revenue secondRevenue;

        @BeforeEach
        void setUp() {
            firstRevenue = new Revenue();
            secondRevenue = new Revenue();
        }

        @Test
        void shouldReturnMappedRevenueListWhenRevenueExists() {
            RevenueDto secondDto = new RevenueDto(null, null, OLD_AMOUNT, OLD_CATEGORY, null, OLD_DESCRIPTION, null, null);
            when(revenueRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstRevenue, secondRevenue));
            when(revenueMapper.mapRevenueToDto(firstRevenue)).thenReturn(revenueDto);
            when(revenueMapper.mapRevenueToDto(secondRevenue)).thenReturn(secondDto);

            List<RevenueDto> result = revenueService.getRevenue(USER_ID);

            assertThat(result).containsExactly(revenueDto, secondDto);
        }

        @Test
        void shouldReturnEmptyListWhenNoRevenueExists() {
            when(revenueRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<RevenueDto> result = revenueService.getRevenue(USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(revenueMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(revenueRepository.findAllByUserId(USER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> revenueService.getRevenue(USER_ID));

            verifyNoInteractions(revenueMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(revenueRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstRevenue));
            when(revenueMapper.mapRevenueToDto(firstRevenue)).thenThrow(new IllegalArgumentException("mapping failed"));

            assertThrows(IllegalArgumentException.class, () -> revenueService.getRevenue(USER_ID));
        }
    }

    @Nested
    class DeleteRevenue {

        private Revenue revenue;

        @BeforeEach
        void setUp() {
            revenue = new Revenue();
            revenue.setId(REVENUE_ID);
            revenue.setUserId(USER_ID);
            revenue.setAmount(AMOUNT);
            revenue.setCategory(CATEGORY);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeCannotBeResolved() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE))
                    .thenThrow(new IllegalArgumentException("invalid code"));

            assertThrows(IllegalArgumentException.class,
                    () -> revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(authBackendClient, revenueManagerService, walletService, revenueRepository,
                    outboxService, autoPaymentsService);
        }

        @Nested
        class WhenAuthorizationIsConfirmed {

            @BeforeEach
            void setUp() {
                when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationConfirmationFails() {
                doThrow(new IllegalStateException("authorization denied")).when(authBackendClient)
                        .confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);

                assertThrows(IllegalStateException.class,
                        () -> revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE));

                verifyNoInteractions(revenueManagerService, walletService, revenueRepository, outboxService, autoPaymentsService);
            }

            @Test
            void shouldThrowExceptionWhenRevenueIsNotFoundForUser() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID))
                        .thenThrow(new RequestedEntityNotFoundException("Revenue not found"));

                assertThrows(RequestedEntityNotFoundException.class,
                        () -> revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE));

                verify(revenueRepository, never()).delete(any(Revenue.class));
                verifyNoInteractions(walletService, outboxService, autoPaymentsService);
            }

            @Nested
            class WhenRevenueExists {

                @BeforeEach
                void setUp() {
                    when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(revenue);
                }

                @Test
                void shouldConfirmAuthorizationCodeWhenRevenueIsDeleted() {
                    revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE);

                    verify(authBackendClient).confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);
                }

                @Test
                void shouldRollbackPiggyBankAutomationWhenRevenueIsDeleted() {
                    revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE);

                    verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.ROLLBACK);
                }

                @Test
                void shouldRemoveAmountFromWalletWhenRevenueIsDeleted() {
                    revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE);

                    verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                }

                @Test
                void shouldSaveDeletedRevenueEventWhenRevenueIsDeleted() {
                    revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE);

                    verifyOutboxEvent(new RevenueActivityEvent(USER_ID, RevenueActivityType.DELETED_REVENUE, AMOUNT, CATEGORY,
                            null, null, LocalDateTime.now()));
                }

                @Test
                void shouldDeleteRevenueWhenRevenueIsDeleted() {
                    revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE);

                    verify(revenueRepository).delete(revenue);
                }

                @Test
                void shouldProcessStepsInOrderWhenRevenueIsDeleted() {
                    revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE);

                    InOrder inOrder = inOrder(authBackendClient, revenueManagerService, autoPaymentsService,
                            walletService, outboxService, revenueRepository);
                    inOrder.verify(authBackendClient).confirmAuthorizationCode(USER_ID, resolvedAuthorizationCode);
                    inOrder.verify(revenueManagerService).getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID);
                    inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.ROLLBACK);
                    inOrder.verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                    inOrder.verify(outboxService).save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), any());
                    inOrder.verify(revenueRepository).delete(revenue);
                }

                @Test
                void shouldThrowExceptionWhenPiggyBankRollbackFails() {
                    doThrow(new IllegalStateException("rollback failed")).when(autoPaymentsService)
                            .handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.ROLLBACK);

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE));

                    verifyNoInteractions(walletService, outboxService);
                    verify(revenueRepository, never()).delete(any(Revenue.class));
                }

                @Test
                void shouldThrowExceptionWhenWalletUpdateFails() {
                    doThrow(new IllegalStateException("wallet failed")).when(walletService)
                            .removeBalanceFromWallet(USER_ID, AMOUNT);

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE));

                    verifyNoInteractions(outboxService);
                    verify(revenueRepository, never()).delete(any(Revenue.class));
                }

                @Test
                void shouldThrowExceptionWhenOutboxServiceFails() {
                    doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                            .save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), any());

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE));

                    verify(revenueRepository, never()).delete(any(Revenue.class));
                }

                @Test
                void shouldThrowExceptionWhenRepositoryDeleteFails() {
                    doThrow(new IllegalStateException("db failed")).when(revenueRepository).delete(revenue);

                    assertThrows(IllegalStateException.class,
                            () -> revenueService.deleteRevenue(REVENUE_ID, USER_ID, AUTHORIZATION_CODE));
                }
            }
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllRevenueOfUserWhenDeletingByUserId() {
            revenueService.deleteByUserId(USER_ID);

            verify(revenueRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            doThrow(new IllegalStateException("db failed")).when(revenueRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> revenueService.deleteByUserId(USER_ID));
        }
    }
}