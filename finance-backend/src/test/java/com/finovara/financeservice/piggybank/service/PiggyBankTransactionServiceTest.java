package com.finovara.financeservice.piggybank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.mainaccount.transaction.event.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.model.PiggyBankGoalType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.piggybank.goalplanner.service.GoalPlannerService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PiggyBankTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 10L;
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("1000.00");
    private static final String AUTH_CODE = "123456";
    private static final String SOURCE_EVENT_ID = "event-1";
    private static final String EMAIL = "user@finovara.com";
    private static final PiggyBankActivityType ACTIVITY_TYPE = PiggyBankActivityType.values()[0];
    private static final PiggyBankGoalType GOAL_TYPE = PiggyBankGoalType.values()[0];
    private static final TransactionOrigin MANUAL_ORIGIN = TransactionOrigin.USER_MANUAL;
    private static final TransactionOrigin AUTOMATIC_ORIGIN = Arrays.stream(TransactionOrigin.values())
            .filter(origin -> origin != TransactionOrigin.USER_MANUAL)
            .findFirst()
            .orElseThrow();

    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private GoalCompletionService goalCompletionService;
    @Mock
    private GoalPlannerService goalPlannerService;
    @Mock
    private WalletService walletService;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    @Mock
    private RiskGuardService riskGuardService;
    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private PiggyBankTransactionService service;

    private PiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        piggyBank = PiggyBank.builder()
                .id(PIGGY_BANK_ID)
                .name("Vacation")
                .amount(BigDecimal.ZERO)
                .goalAmount(GOAL_AMOUNT)
                .goalType(GOAL_TYPE)
                .userId(USER_ID)
                .build();
    }

    @Nested
    class AddBalanceToPiggyBank {

        @Nested
        class WhenOriginIsUserManual {

            @BeforeEach
            void setUp() {
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(EMAIL);
                when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            }

            @Test
            void shouldIncreasePiggyBankAmountWhenOriginIsUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                assertEquals(0, AMOUNT.compareTo(piggyBank.getAmount()));
            }

            @Test
            void shouldGuardRiskWhenOriginIsUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                verify(riskGuardService).guard(USER_ID, RiskTriggerType.PIGGY_BANK, AMOUNT, null, EMAIL, SOURCE_EVENT_ID, servletRequest);
            }

            @Test
            void shouldConfirmAuthorizationCodeWhenOriginIsUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
                verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldRemoveBalanceFromWalletWhenOriginIsUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldPublishTransactionAndProgressEventsWhenOriginIsUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                verify(outboxService).save(eq("PiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("piggybank.transaction.created"),
                        any(PiggyBankActivityEvent.class));
                verify(outboxService).save(eq("PiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("piggybank.calculate-progress"),
                        any(PiggyBankProgressEvent.class));
            }

            @Test
            void shouldNotHandleGoalCompletionWhenGoalIsNotReached() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                verifyNoInteractions(goalCompletionService);
            }

            @Test
            void shouldHandleGoalCompletionWhenGoalIsReached() {
                piggyBank.setAmount(new BigDecimal("900.00"));

                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                verify(goalCompletionService).handleGoalCompletion(USER_ID);
            }

            @Test
            void shouldCheckGoalPlannerCompletionWhenOriginIsUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN);

                verify(goalPlannerService).checkAndMarkGoalCompletion(piggyBank);
            }
        }

        @Nested
        class WhenOriginIsNotUserManual {

            @BeforeEach
            void setUp() {
                when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            }

            @Test
            void shouldSkipRiskGuardAndAuthorizationWhenOriginIsNotUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, null, SOURCE_EVENT_ID, null, AUTOMATIC_ORIGIN);

                verifyNoInteractions(riskGuardService, authBackendClient, additionalAuthorizationCodeResolver);
            }

            @Test
            void shouldIncreasePiggyBankAmountWhenOriginIsNotUserManual() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, null, SOURCE_EVENT_ID, null, AUTOMATIC_ORIGIN);

                assertEquals(0, AMOUNT.compareTo(piggyBank.getAmount()));
                verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldHandleGoalCompletionWhenGoalIsReachedAndOriginIsNotUserManual() {
                piggyBank.setAmount(new BigDecimal("900.00"));

                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE, null, SOURCE_EVENT_ID, null, AUTOMATIC_ORIGIN);

                verify(goalCompletionService).handleGoalCompletion(USER_ID);
                verify(goalPlannerService).checkAndMarkGoalCompletion(piggyBank);
            }
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            assertThrows(InvalidInputException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.ZERO, ACTIVITY_TYPE,
                    AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN));

            verifyNoInteractions(walletService, piggyBankManagerService, riskGuardService, authBackendClient, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThrows(InvalidInputException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, new BigDecimal("-1.00"), ACTIVITY_TYPE,
                    AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN));

            verifyNoInteractions(walletService, piggyBankManagerService, riskGuardService, authBackendClient, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRiskGuardRejectsTransaction() {
            when(authBackendClient.getUserEmail(USER_ID)).thenReturn(EMAIL);
            doThrow(new IllegalStateException("Risk")).when(riskGuardService)
                    .guard(USER_ID, RiskTriggerType.PIGGY_BANK, AMOUNT, null, EMAIL, SOURCE_EVENT_ID, servletRequest);

            assertThrows(IllegalStateException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE,
                    AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN));

            verifyNoInteractions(walletService, piggyBankManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            when(authBackendClient.getUserEmail(USER_ID)).thenReturn(EMAIL);
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE,
                    AUTH_CODE, SOURCE_EVENT_ID, servletRequest, MANUAL_ORIGIN));

            verifyNoInteractions(walletService, piggyBankManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenWalletRemovalFails() {
            doThrow(new IllegalStateException("Insufficient wallet funds")).when(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE,
                    null, SOURCE_EVENT_ID, null, AUTOMATIC_ORIGIN));

            verifyNoInteractions(piggyBankManagerService, outboxService, goalPlannerService, goalCompletionService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, ACTIVITY_TYPE,
                    null, SOURCE_EVENT_ID, null, AUTOMATIC_ORIGIN));

            verifyNoInteractions(outboxService, goalPlannerService, goalCompletionService);
        }
    }

    @Nested
    class RemoveBalanceFromPiggyBank {

        @Nested
        class WhenFundsAreSufficient {

            @BeforeEach
            void setUp() {
                piggyBank.setAmount(new BigDecimal("500.00"));
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(EMAIL);
                when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            }

            @Test
            void shouldDecreasePiggyBankAmountWhenFundsAreSufficient() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE, SOURCE_EVENT_ID, servletRequest);

                assertEquals(0, new BigDecimal("400.00").compareTo(piggyBank.getAmount()));
            }

            @Test
            void shouldLeaveZeroBalanceWhenRemovingWholeAmount() {
                piggyBank.setAmount(AMOUNT);

                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE, SOURCE_EVENT_ID, servletRequest);

                assertEquals(0, BigDecimal.ZERO.compareTo(piggyBank.getAmount()));
            }

            @Test
            void shouldConfirmAuthorizationCodeWhenFundsAreSufficient() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE, SOURCE_EVENT_ID, servletRequest);

                verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
                verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldGuardRiskWhenFundsAreSufficient() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE, SOURCE_EVENT_ID, servletRequest);

                verify(riskGuardService).guard(USER_ID, RiskTriggerType.PIGGY_BANK, AMOUNT, null, EMAIL, SOURCE_EVENT_ID, servletRequest);
            }

            @Test
            void shouldAddBalanceToWalletWhenFundsAreSufficient() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE, SOURCE_EVENT_ID, servletRequest);

                verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldPublishTransactionAndProgressEventsWhenFundsAreSufficient() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE, SOURCE_EVENT_ID, servletRequest);

                verify(outboxService).save(eq("PiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("piggybank.transaction.created"),
                        any(PiggyBankActivityEvent.class));
                verify(outboxService).save(eq("PiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("piggybank.calculate-progress"),
                        any(PiggyBankProgressEvent.class));
            }

            @Test
            void shouldCheckGoalPlannerCompletionWhenFundsAreSufficient() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE, SOURCE_EVENT_ID, servletRequest);

                verify(goalPlannerService).checkAndMarkGoalCompletion(piggyBank);
            }
        }

        @Test
        void shouldThrowExceptionWhenFundsAreInsufficient() {
            piggyBank.setAmount(new BigDecimal("50.00"));
            when(authBackendClient.getUserEmail(USER_ID)).thenReturn(EMAIL);
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE,
                    SOURCE_EVENT_ID, servletRequest));

            assertEquals(0, new BigDecimal("50.00").compareTo(piggyBank.getAmount()));
            verifyNoInteractions(outboxService, goalPlannerService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.ZERO, AUTH_CODE,
                    SOURCE_EVENT_ID, servletRequest));

            verifyNoInteractions(walletService, piggyBankManagerService, riskGuardService, authBackendClient, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, new BigDecimal("-5.00"), AUTH_CODE,
                    SOURCE_EVENT_ID, servletRequest));

            verifyNoInteractions(walletService, piggyBankManagerService, riskGuardService, authBackendClient, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE,
                    SOURCE_EVENT_ID, servletRequest));

            verifyNoInteractions(riskGuardService, walletService, piggyBankManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRiskGuardRejectsTransaction() {
            when(authBackendClient.getUserEmail(USER_ID)).thenReturn(EMAIL);
            doThrow(new IllegalStateException("Risk")).when(riskGuardService)
                    .guard(USER_ID, RiskTriggerType.PIGGY_BANK, AMOUNT, null, EMAIL, SOURCE_EVENT_ID, servletRequest);

            assertThrows(IllegalStateException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE,
                    SOURCE_EVENT_ID, servletRequest));

            verifyNoInteractions(walletService, piggyBankManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(authBackendClient.getUserEmail(USER_ID)).thenReturn(EMAIL);
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, AUTH_CODE,
                    SOURCE_EVENT_ID, servletRequest));

            verifyNoInteractions(outboxService, goalPlannerService);
        }
    }
}