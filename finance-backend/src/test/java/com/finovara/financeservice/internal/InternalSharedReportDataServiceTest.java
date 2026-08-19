package com.finovara.financeservice.internal;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalSharedReportDataServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);
    private static final int PAGE_SIZE = 5;

    @Mock
    private SharedExpenseRepository expenseRepository;

    @Mock
    private SharedRevenueRepository revenueRepository;

    @Mock
    private SharedWalletRepository sharedWalletRepository;

    private InternalSharedReportDataService service;

    @BeforeEach
    void setUp() {
        service = new InternalSharedReportDataService(expenseRepository, revenueRepository, sharedWalletRepository);
    }

    @Nested
    class SumExpenses {

        @Test
        void shouldReturnSumExpensesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("125.50");
            when(expenseRepository.sumExpensesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO)).thenReturn(Optional.of(expected));

            BigDecimal result = service.sumExpenses(OWNER_ID, MEMBER_ID, FROM, TO);

            assertEquals(expected, result);
            verify(expenseRepository).sumExpensesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO);
            verifyNoInteractions(revenueRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnZeroWhenRepositoryReturnsEmptyOptional() {
            when(expenseRepository.sumExpensesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO)).thenReturn(Optional.empty());

            BigDecimal result = service.sumExpenses(OWNER_ID, MEMBER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result);
            verify(expenseRepository).sumExpensesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO);
            verifyNoInteractions(revenueRepository, sharedWalletRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.sumExpensesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumExpenses(OWNER_ID, MEMBER_ID, FROM, TO));

            verify(expenseRepository).sumExpensesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO);
        }
    }

    @Nested
    class HighestExpenses {

        @Test
        void shouldReturnHighestExpensesWhenRepositoryReturnsResults() {
            List<HighestExpenseDto> expected = List.of(mock(HighestExpenseDto.class), mock(HighestExpenseDto.class));

            when(expenseRepository.findHighestExpensesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestExpenseDto> result = service.highestExpenses(OWNER_ID, MEMBER_ID, FROM, TO, PAGE_SIZE);

            assertSame(expected, result);
            verify(expenseRepository).findHighestExpensesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any());
            verifyNoInteractions(revenueRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            List<HighestExpenseDto> expected = List.of();

            when(expenseRepository.findHighestExpensesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestExpenseDto> result = service.highestExpenses(OWNER_ID, MEMBER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(expected, result);
            verify(expenseRepository).findHighestExpensesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any());
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsInvalid() {
            assertThrows(IllegalArgumentException.class, () -> service.highestExpenses(OWNER_ID, MEMBER_ID, FROM, TO, 0));
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsNegative() {
            assertThrows(IllegalArgumentException.class, () -> service.highestExpenses(OWNER_ID, MEMBER_ID, FROM, TO, -1));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.findHighestExpensesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.highestExpenses(OWNER_ID, MEMBER_ID, FROM, TO, PAGE_SIZE));

            verify(expenseRepository).findHighestExpensesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any());
        }
    }

    @Nested
    class ExpensesByCategory {

        @Test
        void shouldReturnSumOfExpensesWhenExpensesExist() {
            SharedExpense firstExpense = mock(SharedExpense.class);
            SharedExpense secondExpense = mock(SharedExpense.class);

            when(firstExpense.getAmount()).thenReturn(new BigDecimal("10.50"));
            when(secondExpense.getAmount()).thenReturn(new BigDecimal("20.25"));
            when(expenseRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of(firstExpense, secondExpense));

            BigDecimal result = service.expensesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD);

            assertEquals(new BigDecimal("30.75"), result);
            verify(expenseRepository).findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD);
            verify(firstExpense).getAmount();
            verify(secondExpense).getAmount();
            verifyNoInteractions(revenueRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnZeroWhenNoExpensesExist() {
            when(expenseRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of());

            BigDecimal result = service.expensesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD);

            assertEquals(BigDecimal.ZERO, result);
            verify(expenseRepository).findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD);
        }

        @Test
        void shouldReturnSingleExpenseAmountWhenOneExpenseExists() {
            SharedExpense expense = mock(SharedExpense.class);
            BigDecimal amount = new BigDecimal("99.99");

            when(expense.getAmount()).thenReturn(amount);
            when(expenseRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of(expense));

            BigDecimal result = service.expensesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD);

            assertEquals(amount, result);
            verify(expense).getAmount();
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.expensesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD));
        }

        @Test
        void shouldThrowExceptionWhenExpenseAmountIsNull() {
            SharedExpense expense = mock(SharedExpense.class);

            when(expense.getAmount()).thenReturn(null);
            when(expenseRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of(expense));

            assertThrows(NullPointerException.class, () -> service.expensesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, ExpenseCategory.FOOD));
        }
    }

    @Nested
    class SumAllExpenses {

        @Test
        void shouldReturnSumAllExpensesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("500.00");

            when(expenseRepository.sumAllExpensesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(expected);

            BigDecimal result = service.sumAllExpenses(OWNER_ID, MEMBER_ID);

            assertEquals(expected, result);
            verify(expenseRepository).sumAllExpensesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verifyNoInteractions(revenueRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(expenseRepository.sumAllExpensesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(null);

            BigDecimal result = service.sumAllExpenses(OWNER_ID, MEMBER_ID);

            assertEquals(null, result);
            verify(expenseRepository).sumAllExpensesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.sumAllExpensesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumAllExpenses(OWNER_ID, MEMBER_ID));
        }
    }

    @Nested
    class ExpensesGroupedByDate {

        @Test
        void shouldReturnExpensesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class), mock(DailyCashDto.class));

            when(expenseRepository.sumExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.expensesGroupedByDate(OWNER_ID, MEMBER_ID);

            assertSame(expected, result);
            verify(expenseRepository).sumExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verifyNoInteractions(revenueRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            List<DailyCashDto> expected = List.of();

            when(expenseRepository.sumExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.expensesGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(expected, result);
            verify(expenseRepository).sumExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(expenseRepository.sumExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.expensesGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(null, result);
            verify(expenseRepository).sumExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.sumExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.expensesGroupedByDate(OWNER_ID, MEMBER_ID));
        }
    }

    @Nested
    class ExpensesAvgGroupedByDate {

        @Test
        void shouldReturnAverageExpensesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class));

            when(expenseRepository.avgExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(OWNER_ID, MEMBER_ID);

            assertSame(expected, result);
            verify(expenseRepository).avgExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verifyNoInteractions(revenueRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            when(expenseRepository.avgExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(List.of());

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(List.of(), result);
            verify(expenseRepository).avgExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(expenseRepository.avgExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(null, result);
            verify(expenseRepository).avgExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.avgExpensesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.expensesAvgGroupedByDate(OWNER_ID, MEMBER_ID));
        }
    }

    @Nested
    class SumRevenues {

        @Test
        void shouldReturnSumRevenuesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("250.75");

            when(revenueRepository.sumRevenuesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO)).thenReturn(Optional.of(expected));

            BigDecimal result = service.sumRevenues(OWNER_ID, MEMBER_ID, FROM, TO);

            assertEquals(expected, result);
            verify(revenueRepository).sumRevenuesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO);
            verifyNoInteractions(expenseRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnZeroWhenRepositoryReturnsEmptyOptional() {
            when(revenueRepository.sumRevenuesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO)).thenReturn(Optional.empty());

            BigDecimal result = service.sumRevenues(OWNER_ID, MEMBER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result);
            verify(revenueRepository).sumRevenuesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.sumRevenuesByOwnerIdOrMemberIdAndDateRange(OWNER_ID, MEMBER_ID, FROM, TO)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumRevenues(OWNER_ID, MEMBER_ID, FROM, TO));
        }
    }

    @Nested
    class HighestRevenues {

        @Test
        void shouldReturnHighestRevenuesWhenRepositoryReturnsResults() {
            List<HighestRevenueDto> expected = List.of(mock(HighestRevenueDto.class), mock(HighestRevenueDto.class));

            when(revenueRepository.findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestRevenueDto> result = service.highestRevenues(OWNER_ID, MEMBER_ID, FROM, TO, PAGE_SIZE);

            assertSame(expected, result);
            verify(revenueRepository).findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any());
            verifyNoInteractions(expenseRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            List<HighestRevenueDto> expected = List.of();

            when(revenueRepository.findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestRevenueDto> result = service.highestRevenues(OWNER_ID, MEMBER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(expected, result);
            verify(revenueRepository).findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any());
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsInvalid() {
            assertThrows(IllegalArgumentException.class, () -> service.highestRevenues(OWNER_ID, MEMBER_ID, FROM, TO, 0));
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsNegative() {
            assertThrows(IllegalArgumentException.class, () -> service.highestRevenues(OWNER_ID, MEMBER_ID, FROM, TO, -1));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(eq(OWNER_ID), eq(MEMBER_ID), eq(FROM), eq(TO), any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.highestRevenues(OWNER_ID, MEMBER_ID, FROM, TO, PAGE_SIZE));
        }
    }

    @Nested
    class RevenuesByCategory {

        @Test
        void shouldReturnSumOfRevenuesWhenRevenuesExist() {
            SharedRevenue firstRevenue = mock(SharedRevenue.class);
            SharedRevenue secondRevenue = mock(SharedRevenue.class);

            when(firstRevenue.getAmount()).thenReturn(new BigDecimal("100.50"));
            when(secondRevenue.getAmount()).thenReturn(new BigDecimal("200.25"));
            when(revenueRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of(firstRevenue, secondRevenue));

            BigDecimal result = service.revenuesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY);

            assertEquals(new BigDecimal("300.75"), result);
            verify(revenueRepository).findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY);
            verify(firstRevenue).getAmount();
            verify(secondRevenue).getAmount();
            verifyNoInteractions(expenseRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnZeroWhenNoRevenuesExist() {
            when(revenueRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of());

            BigDecimal result = service.revenuesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY);

            assertEquals(BigDecimal.ZERO, result);
            verify(revenueRepository).findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY);
        }

        @Test
        void shouldReturnSingleRevenueAmountWhenOneRevenueExists() {
            SharedRevenue revenue = mock(SharedRevenue.class);
            BigDecimal amount = new BigDecimal("999.99");

            when(revenue.getAmount()).thenReturn(amount);
            when(revenueRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of(revenue));

            BigDecimal result = service.revenuesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY);

            assertEquals(amount, result);
            verify(revenue).getAmount();
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.revenuesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY));
        }

        @Test
        void shouldThrowExceptionWhenRevenueAmountIsNull() {
            SharedRevenue revenue = mock(SharedRevenue.class);

            when(revenue.getAmount()).thenReturn(null);
            when(revenueRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of(revenue));

            assertThrows(NullPointerException.class, () -> service.revenuesByCategory(OWNER_ID, MEMBER_ID, FROM, TO, RevenueCategory.SALARY));
        }
    }

    @Nested
    class SumAllRevenues {

        @Test
        void shouldReturnSumAllRevenuesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("800.00");

            when(revenueRepository.sumAllRevenuesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(expected);

            BigDecimal result = service.sumAllRevenues(OWNER_ID, MEMBER_ID);

            assertEquals(expected, result);
            verify(revenueRepository).sumAllRevenuesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verifyNoInteractions(expenseRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(revenueRepository.sumAllRevenuesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(null);

            BigDecimal result = service.sumAllRevenues(OWNER_ID, MEMBER_ID);

            assertEquals(null, result);
            verify(revenueRepository).sumAllRevenuesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.sumAllRevenuesByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumAllRevenues(OWNER_ID, MEMBER_ID));
        }
    }

    @Nested
    class RevenuesGroupedByDate {

        @Test
        void shouldReturnRevenuesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class), mock(DailyCashDto.class));

            when(revenueRepository.sumRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.revenuesGroupedByDate(OWNER_ID, MEMBER_ID);

            assertSame(expected, result);
            verify(revenueRepository).sumRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verifyNoInteractions(expenseRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            when(revenueRepository.sumRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(List.of());

            List<DailyCashDto> result = service.revenuesGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(List.of(), result);
            verify(revenueRepository).sumRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(revenueRepository.sumRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.revenuesGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(null, result);
            verify(revenueRepository).sumRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.sumRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.revenuesGroupedByDate(OWNER_ID, MEMBER_ID));
        }
    }

    @Nested
    class RevenuesAvgGroupedByDate {

        @Test
        void shouldReturnAverageRevenuesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class));

            when(revenueRepository.avgRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(OWNER_ID, MEMBER_ID);

            assertSame(expected, result);
            verify(revenueRepository).avgRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verifyNoInteractions(expenseRepository, sharedWalletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            when(revenueRepository.avgRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(List.of());

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(List.of(), result);
            verify(revenueRepository).avgRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(revenueRepository.avgRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(OWNER_ID, MEMBER_ID);

            assertEquals(null, result);
            verify(revenueRepository).avgRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.avgRevenuesGroupedByDateForOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.revenuesAvgGroupedByDate(OWNER_ID, MEMBER_ID));
        }
    }

    @Nested
    class WalletBalance {

        @Test
        void shouldReturnWalletBalanceWhenWalletExists() {
            SharedWallet wallet = mock(SharedWallet.class);
            BigDecimal expected = new BigDecimal("1500.00");

            when(wallet.getBalance()).thenReturn(expected);
            when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(Optional.of(wallet));

            BigDecimal result = service.walletBalance(OWNER_ID, MEMBER_ID);

            assertEquals(expected, result);
            verify(sharedWalletRepository).findByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verify(wallet).getBalance();
            verifyNoInteractions(expenseRepository, revenueRepository);
        }

        @Test
        void shouldReturnZeroWhenWalletDoesNotExist() {
            when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenReturn(Optional.empty());

            BigDecimal result = service.walletBalance(OWNER_ID, MEMBER_ID);

            assertEquals(BigDecimal.ZERO, result);
            verify(sharedWalletRepository).findByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID);
            verifyNoInteractions(expenseRepository, revenueRepository);
        }


        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, MEMBER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.walletBalance(OWNER_ID, MEMBER_ID));
        }
    }
}
