package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service.GoalAchievedNotificationService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.piggybank.SharedPiggyBankManager;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedPiggyBankTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final String OUTBOX_AGGREGATE = "SharedAccountPiggyBank";
    private static final String OUTBOX_TOPIC = "shared-account.activity";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(50);
    private static final BigDecimal GOAL_AMOUNT = BigDecimal.valueOf(1000);

    @Mock
    private SharedPiggyBankManager sharedPiggyBankManager;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private GoalAchievedNotificationService goalAchievedNotificationService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountParticipantsResponse participants;

    @InjectMocks
    private SharedPiggyBankTransactionService service;

    private SharedPiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        piggyBank = spy(SharedPiggyBank.builder()
                .id(PIGGY_BANK_ID)
                .name("Vacation")
                .amount(BigDecimal.valueOf(100))
                .goalAmount(GOAL_AMOUNT)
                .ownerId(OWNER_ID)
                .memberId(MEMBER_ID)
                .build());
    }

    private BigDecimal expectedPercentage(SharedPiggyBank target) {
        return BigDecimal.valueOf(PiggyBankCalculator.calculateSharedPiggyBankProgress(target))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Nested
    class AddBalanceToPiggyBank {

        @Nested
        class Persisted {

            @BeforeEach
            void setUp() {
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            }

            @Test
            void shouldIncreasePiggyBankAmountWhenDepositMade() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                assertEquals(BigDecimal.valueOf(150), piggyBank.getAmount());
            }

            @Test
            void shouldReturnPercentageWithTwoDecimalPlacesWhenDepositMade() {
                BigDecimal result = service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                assertEquals(expectedPercentage(piggyBank), result);
                assertEquals(2, result.scale());
            }

            @Test
            void shouldRemoveBalanceFromWalletWhenDepositMade() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldLogDepositActivityWhenDepositMade() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(PIGGY_BANK_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
            }

            @Test
            void shouldHandleGoalAchievedWhenDepositMade() {
                service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(goalAchievedNotificationService).handleGoalAchieved(USER_ID, piggyBank);
            }
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, null));
            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedPiggyBankManager, outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            assertThrows(InvalidInputException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.ZERO));
            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedPiggyBankManager, outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThrows(InvalidInputException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.valueOf(-5)));
            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedPiggyBankManager, outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenWalletBalanceIsInsufficient() {
            doThrow(new IllegalStateException()).when(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(sharedPiggyBankManager, outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFound() {
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(outboxService, goalAchievedNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsNotFound() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verifyNoInteractions(sharedWalletService, sharedPiggyBankManager, outboxService, goalAchievedNotificationService);
        }
    }

    @Nested
    class RemoveBalanceFromPiggyBank {

        @Nested
        class Persisted {

            @BeforeEach
            void setUp() {
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            }

            @Test
            void shouldDecreasePiggyBankAmountWhenWithdrawalMade() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                assertEquals(BigDecimal.valueOf(50), piggyBank.getAmount());
            }

            @Test
            void shouldReturnPercentageWithTwoDecimalPlacesWhenWithdrawalMade() {
                BigDecimal result = service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                assertEquals(expectedPercentage(piggyBank), result);
                assertEquals(2, result.scale());
            }

            @Test
            void shouldLockWalletAndAddBalanceToWalletWhenWithdrawalMade() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(sharedWalletService).lockWalletForUpdate(USER_ID);
                verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldLogWithdrawalActivityWhenWithdrawalMade() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(PIGGY_BANK_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
            }

            @Test
            void shouldResetGoalAchievedNotifiedWhenAmountDropsBelowGoal() {
                piggyBank.setAmount(BigDecimal.valueOf(1000));

                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(piggyBank).setGoalAchievedNotified(false);
            }

            @Test
            void shouldNotResetGoalAchievedNotifiedWhenAmountStaysAboveGoal() {
                piggyBank.setAmount(BigDecimal.valueOf(1500));

                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(piggyBank, never()).setGoalAchievedNotified(false);
            }

            @Test
            void shouldNotResetGoalAchievedNotifiedWhenAmountEqualsGoalAfterWithdrawal() {
                piggyBank.setAmount(BigDecimal.valueOf(1050));

                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

                verify(piggyBank, never()).setGoalAchievedNotified(false);
            }

            @Test
            void shouldWithdrawWholeBalanceWhenAmountEqualsPiggyBankAmount() {
                service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.valueOf(100));

                assertEquals(0, BigDecimal.ZERO.compareTo(piggyBank.getAmount()));
                verify(sharedWalletService).addBalanceToWallet(USER_ID, BigDecimal.valueOf(100));
            }
        }

        @Test
        void shouldNotResetGoalAchievedNotifiedWhenGoalAmountIsNull() {
            SharedPiggyBank withoutGoal = spy(SharedPiggyBank.builder()
                    .id(PIGGY_BANK_ID)
                    .amount(BigDecimal.valueOf(100))
                    .goalAmount(null)
                    .build());
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(withoutGoal);

            service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT);

            verify(withoutGoal, never()).setGoalAchievedNotified(false);
            assertEquals(BigDecimal.valueOf(50), withoutGoal.getAmount());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, null));
            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedPiggyBankManager, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.ZERO));
            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedPiggyBankManager, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.valueOf(-5)));
            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedPiggyBankManager, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenFundsAreInsufficient() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, BigDecimal.valueOf(100.01)));
            assertEquals(BigDecimal.valueOf(100), piggyBank.getAmount());
            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFound() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.removeBalanceFromPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT));
            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verifyNoInteractions(outboxService);
        }
    }
}