package com.finovara.securitymonitoring.transaction.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionProfileUpdateServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2025, 1, 15, 10, 30);

    @Mock
    private TransactionProfileRepository transactionProfileRepository;

    @InjectMocks
    private TransactionProfileUpdateService service;

    @Nested
    class HandleExpenseEvent {

        private ExpenseActivityEvent event;
        private TransactionProfile profile;

        @BeforeEach
        void setUp() {
            event = mock(ExpenseActivityEvent.class);
            profile = new TransactionProfile();
            profile.setExpenseCount(0L);
        }

        @Test
        void shouldIgnoreEventWhenTypeIsNotAddedExpense() {
            when(event.type()).thenReturn(ExpenseActivityType.values()[0] == ExpenseActivityType.ADDED_EXPENSE
                    ? ExpenseActivityType.values()[ExpenseActivityType.values().length - 1]
                    : ExpenseActivityType.values()[0]);

            service.handleExpenseEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldSetAmountAsAverageWhenFirstExpenseAndAverageIsNull() {
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("100.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("100.00"), profile.getAverageExpenseAmount());
            assertEquals(1L, profile.getExpenseCount());
        }

        @Test
        void shouldSetAmountAsAverageWhenCountIsZeroAndAverageIsNotNull() {
            profile.setAverageExpenseAmount(new BigDecimal("50.00"));
            profile.setExpenseCount(0L);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("30.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("30.00"), profile.getAverageExpenseAmount());
            assertEquals(1L, profile.getExpenseCount());
        }

        @Test
        void shouldCalculateNewAverageWhenExpensesAlreadyExist() {
            profile.setAverageExpenseAmount(new BigDecimal("100.00"));
            profile.setExpenseCount(2L);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("160.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("120.00"), profile.getAverageExpenseAmount());
            assertEquals(3L, profile.getExpenseCount());
        }

        @Test
        void shouldRoundAverageHalfUpWhenResultHasMoreThanTwoDecimalPlaces() {
            profile.setAverageExpenseAmount(new BigDecimal("10.00"));
            profile.setExpenseCount(2L);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("10.01"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("10.00"), profile.getAverageExpenseAmount());
        }

        @Test
        void shouldRoundAverageUpWhenFractionIsExactlyHalf() {
            profile.setAverageExpenseAmount(new BigDecimal("0.01"));
            profile.setExpenseCount(1L);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("0.02"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("0.02"), profile.getAverageExpenseAmount());
        }

        @Test
        void shouldUpdateLastExpenseFieldsWhenExpenseAdded() {
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("75.50"));
            when(event.category()).thenReturn(null);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("75.50"), profile.getLastExpenseAmount());
            assertNull(profile.getLastExpenseCategory());
            assertEquals(OCCURRED_AT, profile.getLastExpenseAt());
        }

        @Test
        void shouldSetLargestExpenseWhenLargestExpenseIsNull() {
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("40.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("40.00"), profile.getLargestExpenseAmount());
        }

        @Test
        void shouldUpdateLargestExpenseWhenNewAmountIsGreater() {
            profile.setLargestExpenseAmount(new BigDecimal("40.00"));
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("90.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("90.00"), profile.getLargestExpenseAmount());
        }

        @Test
        void shouldKeepLargestExpenseWhenNewAmountIsSmaller() {
            profile.setLargestExpenseAmount(new BigDecimal("200.00"));
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("90.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("200.00"), profile.getLargestExpenseAmount());
        }

        @Test
        void shouldKeepLargestExpenseWhenNewAmountIsEqual() {
            BigDecimal existingLargest = new BigDecimal("90.00");
            profile.setLargestExpenseAmount(existingLargest);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("90.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(existingLargest, profile.getLargestExpenseAmount());
        }

        @Test
        void shouldSetUpdatedAtWhenExpenseAdded() {
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("10.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            LocalDateTime before = LocalDateTime.now();

            service.handleExpenseEvent(event);

            assertNotNull(profile.getUpdatedAt());
            assertEquals(true, !profile.getUpdatedAt().isBefore(before));
        }

        @Test
        void shouldSaveProfileWhenExpenseAdded() {
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("10.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            ArgumentCaptor<TransactionProfile> captor = ArgumentCaptor.forClass(TransactionProfile.class);
            verify(transactionProfileRepository).save(captor.capture());
            assertEquals(profile, captor.getValue());
            verifyNoMoreInteractions(transactionProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundForExpenseEvent() {
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handleExpenseEvent(event));
        }

        @Test
        void shouldNotSaveProfileWhenProfileNotFoundForExpenseEvent() {
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handleExpenseEvent(event));

            verify(transactionProfileRepository, never()).save(any(TransactionProfile.class));
        }
    }

    @Nested
    class HandleRevenueEvent {

        private RevenueActivityEvent event;
        private TransactionProfile profile;

        @BeforeEach
        void setUp() {
            event = mock(RevenueActivityEvent.class);
            profile = new TransactionProfile();
            profile.setRevenueCount(0L);
        }

        @Test
        void shouldIgnoreEventWhenTypeIsNotAddedRevenue() {
            when(event.type()).thenReturn(RevenueActivityType.values()[0] == RevenueActivityType.ADDED_REVENUE
                    ? RevenueActivityType.values()[RevenueActivityType.values().length - 1]
                    : RevenueActivityType.values()[0]);

            service.handleRevenueEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldSetAmountAsAverageWhenFirstRevenueAndAverageIsNull() {
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("500.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertEquals(new BigDecimal("500.00"), profile.getAverageRevenueAmount());
            assertEquals(1L, profile.getRevenueCount());
        }

        @Test
        void shouldSetAmountAsAverageWhenCountIsZeroAndAverageIsNotNull() {
            profile.setAverageRevenueAmount(new BigDecimal("999.00"));
            profile.setRevenueCount(0L);
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("300.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertEquals(new BigDecimal("300.00"), profile.getAverageRevenueAmount());
            assertEquals(1L, profile.getRevenueCount());
        }

        @Test
        void shouldCalculateNewAverageWhenRevenuesAlreadyExist() {
            profile.setAverageRevenueAmount(new BigDecimal("1000.00"));
            profile.setRevenueCount(3L);
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("2000.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertEquals(new BigDecimal("1250.00"), profile.getAverageRevenueAmount());
            assertEquals(4L, profile.getRevenueCount());
        }

        @Test
        void shouldRoundAverageHalfUpWhenResultHasMoreThanTwoDecimalPlaces() {
            profile.setAverageRevenueAmount(new BigDecimal("10.00"));
            profile.setRevenueCount(2L);
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("10.02"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertEquals(new BigDecimal("10.01"), profile.getAverageRevenueAmount());
        }

        @Test
        void shouldUpdateLastRevenueFieldsWhenRevenueAdded() {
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("1200.00"));
            when(event.category()).thenReturn(null);
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertEquals(new BigDecimal("1200.00"), profile.getLastRevenueAmount());
            assertNull(profile.getLastRevenueCategory());
            assertEquals(OCCURRED_AT, profile.getLastRevenueAt());
        }

        @Test
        void shouldSetUpdatedAtWhenRevenueAdded() {
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("10.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            LocalDateTime before = LocalDateTime.now();

            service.handleRevenueEvent(event);

            assertNotNull(profile.getUpdatedAt());
            assertEquals(true, !profile.getUpdatedAt().isBefore(before));
        }

        @Test
        void shouldSaveProfileWhenRevenueAdded() {
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("10.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            ArgumentCaptor<TransactionProfile> captor = ArgumentCaptor.forClass(TransactionProfile.class);
            verify(transactionProfileRepository).save(captor.capture());
            assertEquals(profile, captor.getValue());
            verifyNoMoreInteractions(transactionProfileRepository);
        }

        @Test
        void shouldNotModifyExpenseFieldsWhenRevenueAdded() {
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(new BigDecimal("10.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertNull(profile.getAverageExpenseAmount());
            assertNull(profile.getLastExpenseAmount());
            assertNull(profile.getLargestExpenseAmount());
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundForRevenueEvent() {
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handleRevenueEvent(event));
        }

        @Test
        void shouldNotSaveProfileWhenProfileNotFoundForRevenueEvent() {
            when(event.type()).thenReturn(RevenueActivityType.ADDED_REVENUE);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handleRevenueEvent(event));

            verify(transactionProfileRepository, never()).save(any(TransactionProfile.class));
        }
    }

    @Nested
    class HandlePiggyBankEvent {

        private PiggyBankActivityEvent event;
        private TransactionProfile profile;

        @BeforeEach
        void setUp() {
            event = mock(PiggyBankActivityEvent.class);
            profile = new TransactionProfile();
        }

        @Test
        void shouldIgnoreEventWhenTypeIsNotAmountAddedDirectly() {
            when(event.type()).thenReturn(PiggyBankActivityType.values()[0] == PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY
                    ? PiggyBankActivityType.values()[PiggyBankActivityType.values().length - 1]
                    : PiggyBankActivityType.values()[0]);

            service.handlePiggyBankEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldUpdateLastDepositFieldsWhenAmountAddedDirectly() {
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(new BigDecimal("150.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            assertEquals(new BigDecimal("150.00"), profile.getLastPiggyBankDepositAmount());
            assertEquals(OCCURRED_AT, profile.getLastPiggyBankDepositAt());
        }

        @Test
        void shouldSetLargestDepositWhenLargestDepositIsNull() {
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(new BigDecimal("150.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            assertEquals(new BigDecimal("150.00"), profile.getLargestPiggyBankDeposit());
        }

        @Test
        void shouldUpdateLargestDepositWhenNewAmountIsGreater() {
            profile.setLargestPiggyBankDeposit(new BigDecimal("100.00"));
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(new BigDecimal("250.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            assertEquals(new BigDecimal("250.00"), profile.getLargestPiggyBankDeposit());
        }

        @Test
        void shouldKeepLargestDepositWhenNewAmountIsSmaller() {
            profile.setLargestPiggyBankDeposit(new BigDecimal("500.00"));
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(new BigDecimal("250.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            assertEquals(new BigDecimal("500.00"), profile.getLargestPiggyBankDeposit());
        }

        @Test
        void shouldKeepLargestDepositWhenNewAmountIsEqual() {
            BigDecimal existingLargest = new BigDecimal("250.00");
            profile.setLargestPiggyBankDeposit(existingLargest);
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(new BigDecimal("250.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            assertEquals(existingLargest, profile.getLargestPiggyBankDeposit());
        }

        @Test
        void shouldSetUpdatedAtWhenAmountAddedDirectly() {
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(new BigDecimal("10.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));
            LocalDateTime before = LocalDateTime.now();

            service.handlePiggyBankEvent(event);

            assertNotNull(profile.getUpdatedAt());
            assertEquals(true, !profile.getUpdatedAt().isBefore(before));
        }

        @Test
        void shouldSaveProfileWhenAmountAddedDirectly() {
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(new BigDecimal("10.00"));
            when(event.occurredAt()).thenReturn(OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            ArgumentCaptor<TransactionProfile> captor = ArgumentCaptor.forClass(TransactionProfile.class);
            verify(transactionProfileRepository).save(captor.capture());
            assertEquals(profile, captor.getValue());
            verifyNoMoreInteractions(transactionProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundForPiggyBankEvent() {
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handlePiggyBankEvent(event));
        }

        @Test
        void shouldNotSaveProfileWhenProfileNotFoundForPiggyBankEvent() {
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handlePiggyBankEvent(event));

            verify(transactionProfileRepository, never()).save(any(TransactionProfile.class));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteProfileWhenUserIdProvided() {
            service.deleteByUserId(USER_ID);

            verify(transactionProfileRepository).deleteByUserId(USER_ID);
            verifyNoMoreInteractions(transactionProfileRepository);
        }

        @Test
        void shouldDeleteProfileWhenUserIdIsNull() {
            service.deleteByUserId(null);

            verify(transactionProfileRepository).deleteByUserId(null);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsOnDelete() {
            doThrow(new IllegalStateException("db error")).when(transactionProfileRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> service.deleteByUserId(USER_ID));
        }
    }
}