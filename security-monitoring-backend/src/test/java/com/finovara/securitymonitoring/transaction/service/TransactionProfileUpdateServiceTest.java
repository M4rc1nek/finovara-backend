package com.finovara.securitymonitoring.transaction.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

        private TransactionProfile profile;

        @BeforeEach
        void setUp() {
            profile = new TransactionProfile();
            profile.setExpenseCount(0L);
            profile.setUserId(USER_ID);
        }

        @Test
        void shouldIgnoreEventWhenTypeIsNotAddedExpense() {
            ExpenseActivityEvent event = new ExpenseActivityEvent(USER_ID, ExpenseActivityType.EDITED_EXPENSE,
                    new BigDecimal("100.00"), ExpenseCategory.FOOD, null, null, OCCURRED_AT);

            service.handleExpenseEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundForExpenseEvent() {
            ExpenseActivityEvent event = new ExpenseActivityEvent(USER_ID, ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("100.00"), ExpenseCategory.FOOD, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handleExpenseEvent(event));
            verify(transactionProfileRepository, never()).save(any(TransactionProfile.class));
        }

        @Test
        void shouldAddFirstExpenseAndPersistUpdatedProfile() {
            ExpenseActivityEvent event = new ExpenseActivityEvent(USER_ID, ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("100.00"), ExpenseCategory.FOOD, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("100.00"), profile.getAverageExpenseAmount());
            assertEquals(1L, profile.getExpenseCount());
            assertEquals(new BigDecimal("100.00"), profile.getLastExpenseAmount());
            assertEquals(ExpenseCategory.FOOD, profile.getLastExpenseCategory());
            assertEquals(OCCURRED_AT, profile.getLastExpenseAt());
            assertEquals(new BigDecimal("100.00"), profile.getLargestExpenseAmount());
            assertNotNull(profile.getUpdatedAt());
            verify(transactionProfileRepository).save(profile);
        }

        @Test
        void shouldCalculateAverageUsingExistingDataAndRoundHalfUp() {
            profile.setAverageExpenseAmount(new BigDecimal("100.00"));
            profile.setExpenseCount(2L);
            profile.setLargestExpenseAmount(new BigDecimal("150.00"));
            ExpenseActivityEvent event = new ExpenseActivityEvent(USER_ID, ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("160.00"), ExpenseCategory.HEALTH, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("120.00"), profile.getAverageExpenseAmount());
            assertEquals(3L, profile.getExpenseCount());
            assertEquals(new BigDecimal("160.00"), profile.getLargestExpenseAmount());
        }

        @Test
        void shouldKeepLargestExpenseWhenNewAmountIsLower() {
            profile.setLargestExpenseAmount(new BigDecimal("200.00"));
            ExpenseActivityEvent event = new ExpenseActivityEvent(USER_ID, ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("90.00"), ExpenseCategory.EDUCATION, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(new BigDecimal("200.00"), profile.getLargestExpenseAmount());
        }

        @Test
        void shouldKeepLargestExpenseWhenNewAmountIsEqual() {
            BigDecimal existingLargest = new BigDecimal("90.00");
            profile.setLargestExpenseAmount(existingLargest);
            ExpenseActivityEvent event = new ExpenseActivityEvent(USER_ID, ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("90.00"), ExpenseCategory.BILLS, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleExpenseEvent(event);

            assertEquals(existingLargest, profile.getLargestExpenseAmount());
        }
    }

    @Nested
    class HandleRevenueEvent {

        private TransactionProfile profile;

        @BeforeEach
        void setUp() {
            profile = new TransactionProfile();
            profile.setRevenueCount(0L);
            profile.setUserId(USER_ID);
        }

        @Test
        void shouldIgnoreEventWhenTypeIsNotAddedRevenue() {
            RevenueActivityEvent event = new RevenueActivityEvent(USER_ID, RevenueActivityType.EDITED_REVENUE,
                    new BigDecimal("500.00"), RevenueCategory.SALARY, null, null, OCCURRED_AT);

            service.handleRevenueEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundForRevenueEvent() {
            RevenueActivityEvent event = new RevenueActivityEvent(USER_ID, RevenueActivityType.ADDED_REVENUE,
                    new BigDecimal("500.00"), RevenueCategory.SALARY, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handleRevenueEvent(event));
            verify(transactionProfileRepository, never()).save(any(TransactionProfile.class));
        }

        @Test
        void shouldAddFirstRevenueAndPersistUpdatedProfile() {
            RevenueActivityEvent event = new RevenueActivityEvent(USER_ID, RevenueActivityType.ADDED_REVENUE,
                    new BigDecimal("500.00"), RevenueCategory.SALARY, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertEquals(new BigDecimal("500.00"), profile.getAverageRevenueAmount());
            assertEquals(1L, profile.getRevenueCount());
            assertEquals(new BigDecimal("500.00"), profile.getLastRevenueAmount());
            assertEquals(RevenueCategory.SALARY, profile.getLastRevenueCategory());
            assertEquals(OCCURRED_AT, profile.getLastRevenueAt());
            assertNotNull(profile.getUpdatedAt());
            verify(transactionProfileRepository).save(profile);
        }

        @Test
        void shouldCalculateAverageWhenRevenueHistoryExists() {
            profile.setAverageRevenueAmount(new BigDecimal("1000.00"));
            profile.setRevenueCount(3L);
            RevenueActivityEvent event = new RevenueActivityEvent(USER_ID, RevenueActivityType.ADDED_REVENUE,
                    new BigDecimal("2000.00"), RevenueCategory.BONUS, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertEquals(new BigDecimal("1250.00"), profile.getAverageRevenueAmount());
            assertEquals(4L, profile.getRevenueCount());
        }

        @Test
        void shouldNotModifyExpenseFieldsWhenRevenueIsAdded() {
            RevenueActivityEvent event = new RevenueActivityEvent(USER_ID, RevenueActivityType.ADDED_REVENUE,
                    new BigDecimal("750.00"), RevenueCategory.BUSINESS, null, null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handleRevenueEvent(event);

            assertNull(profile.getAverageExpenseAmount());
            assertNull(profile.getLastExpenseAmount());
            assertNull(profile.getLargestExpenseAmount());
        }
    }

    @Nested
    class HandlePiggyBankEvent {

        private TransactionProfile profile;

        @BeforeEach
        void setUp() {
            profile = new TransactionProfile();
            profile.setUserId(USER_ID);
        }

        @Test
        void shouldIgnoreNullEvent() {
            assertDoesNotThrow(() -> service.handlePiggyBankEvent(null));
            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldIgnoreEventWhenTypeIsNotAmountAddedDirectly() {
            PiggyBankActivityEvent event = new PiggyBankActivityEvent(USER_ID,
                    PiggyBankActivityType.ADDED_PIGGY_BANK, "Goal name", null, null, new BigDecimal("50.00"), null,
                    OCCURRED_AT);

            service.handlePiggyBankEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldIgnoreEventWhenAmountIsMissing() {
            PiggyBankActivityEvent event = new PiggyBankActivityEvent(USER_ID,
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, "Goal name", null, null, null, null,
                    OCCURRED_AT);

            service.handlePiggyBankEvent(event);

            verify(transactionProfileRepository, never()).findByUserIdForUpdate(anyLong());
            verify(transactionProfileRepository, never()).save(any(TransactionProfile.class));
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundForPiggyBankEvent() {
            PiggyBankActivityEvent event = new PiggyBankActivityEvent(USER_ID,
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, "Goal name", null, null,
                    new BigDecimal("120.00"), null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.handlePiggyBankEvent(event));
            verify(transactionProfileRepository, never()).save(any(TransactionProfile.class));
        }

        @Test
        void shouldUseAmountPaidWhenPresentAndUpdateLargestDeposit() {
            PiggyBankActivityEvent event = new PiggyBankActivityEvent(USER_ID,
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, "Goal name", null, null,
                    new BigDecimal("150.00"), null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            assertEquals(new BigDecimal("150.00"), profile.getLastPiggyBankDepositAmount());
            assertEquals(OCCURRED_AT, profile.getLastPiggyBankDepositAt());
            assertEquals(new BigDecimal("150.00"), profile.getLargestPiggyBankDeposit());
            assertNotNull(profile.getUpdatedAt());
            verify(transactionProfileRepository).save(profile);
        }

        @Test
        void shouldKeepLargestDepositWhenNewAmountIsSmaller() {
            profile.setLargestPiggyBankDeposit(new BigDecimal("500.00"));
            PiggyBankActivityEvent event = new PiggyBankActivityEvent(USER_ID,
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, "Goal name", null, null,
                    new BigDecimal("250.00"), null, OCCURRED_AT);
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(profile));

            service.handlePiggyBankEvent(event);

            assertEquals(new BigDecimal("500.00"), profile.getLargestPiggyBankDeposit());
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteProfileWhenUserExists() {
            TransactionProfile existingProfile = new TransactionProfile();
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(existingProfile));

            service.deleteByUserId(USER_ID);

            verify(transactionProfileRepository).findByUserIdForUpdate(USER_ID);
            verify(transactionProfileRepository).delete(existingProfile);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFoundDuringDelete() {
            when(transactionProfileRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteByUserId(USER_ID));
            verify(transactionProfileRepository, never()).delete(any(TransactionProfile.class));
        }
    }
}
