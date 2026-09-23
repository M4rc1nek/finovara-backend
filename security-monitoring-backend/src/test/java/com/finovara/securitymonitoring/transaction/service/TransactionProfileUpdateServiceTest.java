package com.finovara.securitymonitoring.transaction.service;

import com.finovara.contracts.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProfileUpdateServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionProfileRepository transactionProfileRepository;

    @InjectMocks
    private TransactionProfileUpdateService transactionProfileUpdateService;

    private TransactionProfile profile;

    @BeforeEach
    void setUp() {
        profile = TransactionProfile.builder()
                .id(1L)
                .userId(USER_ID)
                .expenseCount(0L)
                .revenueCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    class HandleExpenseEvent {

        @Test
        void shouldDoNothingWhenEventTypeIsNotAddedExpense() {
            ExpenseActivityEvent event = mock(ExpenseActivityEvent.class);
            when(event.type()).thenReturn(null);

            transactionProfileUpdateService.handleExpenseEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFound() {
            ExpenseActivityEvent event = mock(ExpenseActivityEvent.class);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> transactionProfileUpdateService.handleExpenseEvent(event));
        }

        @Test
        void shouldSetAverageToEventAmountWhenFirstExpense() {
            ExpenseActivityEvent event = mock(ExpenseActivityEvent.class);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(BigDecimal.valueOf(100));
            when(event.category()).thenReturn(ExpenseCategory.values()[0]);
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handleExpenseEvent(event);

            assertEquals(0, BigDecimal.valueOf(100).compareTo(profile.getAverageExpenseAmount()));
            assertEquals(1L, profile.getExpenseCount());
            verify(transactionProfileRepository).save(profile);
        }

        @Test
        void shouldCalculateWeightedAverageWhenSubsequentExpense() {
            profile.setAverageExpenseAmount(BigDecimal.valueOf(50));
            profile.setExpenseCount(2L);
            ExpenseActivityEvent event = mock(ExpenseActivityEvent.class);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(BigDecimal.valueOf(100));
            when(event.category()).thenReturn(ExpenseCategory.values()[0]);
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handleExpenseEvent(event);

            assertEquals(0, new BigDecimal("66.67").compareTo(profile.getAverageExpenseAmount()));
            assertEquals(3L, profile.getExpenseCount());
        }

        @Test
        void shouldSetLargestExpenseWhenCurrentLargestIsNull() {
            profile.setLargestExpenseAmount(null);
            ExpenseActivityEvent event = mock(ExpenseActivityEvent.class);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(BigDecimal.valueOf(200));
            when(event.category()).thenReturn(ExpenseCategory.values()[0]);
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handleExpenseEvent(event);

            assertEquals(0, BigDecimal.valueOf(200).compareTo(profile.getLargestExpenseAmount()));
        }

        @Test
        void shouldUpdateLargestExpenseWhenAmountGreaterThanCurrentLargest() {
            profile.setLargestExpenseAmount(BigDecimal.valueOf(150));
            ExpenseActivityEvent event = mock(ExpenseActivityEvent.class);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(BigDecimal.valueOf(200));
            when(event.category()).thenReturn(ExpenseCategory.values()[0]);
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handleExpenseEvent(event);

            assertEquals(0, BigDecimal.valueOf(200).compareTo(profile.getLargestExpenseAmount()));
        }

        @Test
        void shouldNotUpdateLargestExpenseWhenAmountLessThanOrEqualToCurrentLargest() {
            profile.setLargestExpenseAmount(BigDecimal.valueOf(300));
            ExpenseActivityEvent event = mock(ExpenseActivityEvent.class);
            when(event.type()).thenReturn(ExpenseActivityType.ADDED_EXPENSE);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(BigDecimal.valueOf(200));
            when(event.category()).thenReturn(ExpenseCategory.values()[0]);
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handleExpenseEvent(event);

            assertEquals(0, BigDecimal.valueOf(300).compareTo(profile.getLargestExpenseAmount()));
        }
    }

    @Nested
    class HandleRevenueEvent {

        @Test
        void shouldThrowExceptionWhenProfileNotFound() {
            RevenueActivityEvent event = mock(RevenueActivityEvent.class);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> transactionProfileUpdateService.handleRevenueEvent(event));
        }

        @Test
        void shouldSetAverageToEventAmountWhenFirstRevenue() {
            RevenueActivityEvent event = mock(RevenueActivityEvent.class);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(BigDecimal.valueOf(500));
            when(event.category()).thenReturn(RevenueCategory.values()[0]);
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handleRevenueEvent(event);

            assertEquals(0, BigDecimal.valueOf(500).compareTo(profile.getAverageRevenueAmount()));
            assertEquals(1L, profile.getRevenueCount());
            verify(transactionProfileRepository).save(profile);
        }

        @Test
        void shouldCalculateWeightedAverageWhenSubsequentRevenue() {
            profile.setAverageRevenueAmount(BigDecimal.valueOf(200));
            profile.setRevenueCount(1L);
            RevenueActivityEvent event = mock(RevenueActivityEvent.class);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amount()).thenReturn(BigDecimal.valueOf(400));
            when(event.category()).thenReturn(RevenueCategory.values()[0]);
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handleRevenueEvent(event);

            assertEquals(0, new BigDecimal("300.00").compareTo(profile.getAverageRevenueAmount()));
            assertEquals(2L, profile.getRevenueCount());
        }
    }

    @Nested
    class HandlePiggyBankEvent {

        @Test
        void shouldDoNothingWhenEventTypeIsNotAmountAddedDirectly() {
            PiggyBankActivityEvent event = mock(PiggyBankActivityEvent.class);
            when(event.type()).thenReturn(null);

            transactionProfileUpdateService.handlePiggyBankEvent(event);

            verifyNoInteractions(transactionProfileRepository);
        }

        @Test
        void shouldThrowExceptionWhenProfileNotFound() {
            PiggyBankActivityEvent event = mock(PiggyBankActivityEvent.class);
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> transactionProfileUpdateService.handlePiggyBankEvent(event));
        }

        @Test
        void shouldSetLargestDepositWhenCurrentIsNull() {
            profile.setLargestPiggyBankDeposit(null);
            PiggyBankActivityEvent event = mock(PiggyBankActivityEvent.class);
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(BigDecimal.valueOf(80));
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handlePiggyBankEvent(event);

            assertEquals(0, BigDecimal.valueOf(80).compareTo(profile.getLargestPiggyBankDeposit()));
            assertEquals(0, BigDecimal.valueOf(80).compareTo(profile.getLastPiggyBankDepositAmount()));
        }

        @Test
        void shouldUpdateLargestDepositWhenAmountGreaterThanCurrent() {
            profile.setLargestPiggyBankDeposit(BigDecimal.valueOf(50));
            PiggyBankActivityEvent event = mock(PiggyBankActivityEvent.class);
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(BigDecimal.valueOf(90));
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handlePiggyBankEvent(event);

            assertEquals(0, BigDecimal.valueOf(90).compareTo(profile.getLargestPiggyBankDeposit()));
        }

        @Test
        void shouldNotUpdateLargestDepositWhenAmountLessThanOrEqualToCurrent() {
            profile.setLargestPiggyBankDeposit(BigDecimal.valueOf(120));
            PiggyBankActivityEvent event = mock(PiggyBankActivityEvent.class);
            when(event.type()).thenReturn(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
            when(event.userId()).thenReturn(USER_ID);
            when(event.amountPaid()).thenReturn(BigDecimal.valueOf(90));
            when(event.occurredAt()).thenReturn(LocalDateTime.now());
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

            transactionProfileUpdateService.handlePiggyBankEvent(event);

            assertEquals(0, BigDecimal.valueOf(120).compareTo(profile.getLargestPiggyBankDeposit()));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteProfileForGivenUserId() {
            transactionProfileUpdateService.deleteByUserId(USER_ID);

            verify(transactionProfileRepository, times(1)).deleteByUserId(USER_ID);
        }
    }
}