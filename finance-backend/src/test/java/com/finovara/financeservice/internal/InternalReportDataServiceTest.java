package com.finovara.financeservice.internal;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
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
class InternalReportDataServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);
    private static final int PAGE_SIZE = 5;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private WalletRepository walletRepository;

    private InternalReportDataService service;

    @BeforeEach
    void setUp() {
        service = new InternalReportDataService(expenseRepository, revenueRepository, walletRepository);
    }

    @Nested
    class SumExpenses {

        @Test
        void shouldReturnSumExpensesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("125.50");

            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(expected));

            BigDecimal result = service.sumExpenses(USER_ID, FROM, TO);

            assertEquals(expected, result);
            verify(expenseRepository).sumExpensesByUserAndDateRange(USER_ID, FROM, TO);
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldReturnZeroWhenRepositoryReturnsEmptyOptional() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.empty());

            BigDecimal result = service.sumExpenses(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result);
            verify(expenseRepository).sumExpensesByUserAndDateRange(USER_ID, FROM, TO);
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FROM, TO)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumExpenses(USER_ID, FROM, TO));
        }
    }

    @Nested
    class AvgExpenses {

        @Test
        void shouldReturnAverageExpensesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("62.75");

            when(expenseRepository.avgExpensesByUserIdAndPeriod(USER_ID, FROM, TO)).thenReturn(Optional.of(expected));

            BigDecimal result = service.avgExpenses(USER_ID, FROM, TO);

            assertEquals(expected, result);
            verify(expenseRepository).avgExpensesByUserIdAndPeriod(USER_ID, FROM, TO);
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldReturnZeroWhenRepositoryReturnsEmptyOptional() {
            when(expenseRepository.avgExpensesByUserIdAndPeriod(USER_ID, FROM, TO)).thenReturn(Optional.empty());

            BigDecimal result = service.avgExpenses(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result);
            verify(expenseRepository).avgExpensesByUserIdAndPeriod(USER_ID, FROM, TO);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.avgExpensesByUserIdAndPeriod(USER_ID, FROM, TO)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.avgExpenses(USER_ID, FROM, TO));
        }
    }

    @Nested
    class HighestExpenses {

        @Test
        void shouldReturnHighestExpensesWhenRepositoryReturnsResults() {
            List<HighestExpenseDto> expected = List.of(mock(HighestExpenseDto.class), mock(HighestExpenseDto.class));

            when(expenseRepository.findHighestExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestExpenseDto> result = service.highestExpenses(USER_ID, FROM, TO, PAGE_SIZE);

            assertSame(expected, result);
            verify(expenseRepository).findHighestExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any());
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            List<HighestExpenseDto> expected = List.of();

            when(expenseRepository.findHighestExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestExpenseDto> result = service.highestExpenses(USER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(expected, result);
            verify(expenseRepository).findHighestExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any());
        }

        @Test
        void shouldAcceptPageSizeEqualToOne() {
            List<HighestExpenseDto> expected = List.of(mock(HighestExpenseDto.class));

            when(expenseRepository.findHighestExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestExpenseDto> result = service.highestExpenses(USER_ID, FROM, TO, 1);

            assertSame(expected, result);
            verify(expenseRepository).findHighestExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any());
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsZero() {
            assertThrows(IllegalArgumentException.class, () -> service.highestExpenses(USER_ID, FROM, TO, 0));
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsNegative() {
            assertThrows(IllegalArgumentException.class, () -> service.highestExpenses(USER_ID, FROM, TO, -1));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.findHighestExpensesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.highestExpenses(USER_ID, FROM, TO, PAGE_SIZE));
        }
    }

    @Nested
    class ExpensesByCategory {

        @Test
        void shouldReturnSumOfExpensesWhenExpensesExist() {
            Expense firstExpense = mock(Expense.class);
            Expense secondExpense = mock(Expense.class);

            when(firstExpense.getAmount()).thenReturn(new BigDecimal("10.50"));
            when(secondExpense.getAmount()).thenReturn(new BigDecimal("20.25"));

            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of(firstExpense, secondExpense));

            BigDecimal result = service.expensesByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);

            assertEquals(new BigDecimal("30.75"), result);
            verify(expenseRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);
            verify(firstExpense).getAmount();
            verify(secondExpense).getAmount();
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldReturnZeroWhenNoExpensesExist() {
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of());

            BigDecimal result = service.expensesByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);

            assertEquals(BigDecimal.ZERO, result);
            verify(expenseRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);
        }

        @Test
        void shouldReturnExpenseAmountWhenOneExpenseExists() {
            Expense expense = mock(Expense.class);
            BigDecimal amount = new BigDecimal("99.99");

            when(expense.getAmount()).thenReturn(amount);
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of(expense));

            BigDecimal result = service.expensesByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);

            assertEquals(amount, result);
            verify(expense).getAmount();
        }

        @Test
        void shouldHandleNegativeExpenseAmountWhenRepositoryReturnsNegativeAmount() {
            Expense expense = mock(Expense.class);
            BigDecimal amount = new BigDecimal("-10.50");

            when(expense.getAmount()).thenReturn(amount);
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of(expense));

            BigDecimal result = service.expensesByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD);

            assertEquals(amount, result);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.expensesByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD));
        }

        @Test
        void shouldThrowExceptionWhenExpenseAmountIsNull() {
            Expense expense = mock(Expense.class);

            when(expense.getAmount()).thenReturn(null);
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD)).thenReturn(List.of(expense));

            assertThrows(NullPointerException.class, () -> service.expensesByCategory(USER_ID, FROM, TO, ExpenseCategory.FOOD));
        }
    }

    @Nested
    class SumAllExpenses {

        @Test
        void shouldReturnSumAllExpensesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("500.00");

            when(expenseRepository.sumAllExpensesByUserId(USER_ID)).thenReturn(expected);

            BigDecimal result = service.sumAllExpenses(USER_ID);

            assertEquals(expected, result);
            verify(expenseRepository).sumAllExpensesByUserId(USER_ID);
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(expenseRepository.sumAllExpensesByUserId(USER_ID)).thenReturn(null);

            BigDecimal result = service.sumAllExpenses(USER_ID);

            assertEquals(null, result);
            verify(expenseRepository).sumAllExpensesByUserId(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.sumAllExpensesByUserId(USER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumAllExpenses(USER_ID));
        }
    }

    @Nested
    class ExpensesGroupedByDate {

        @Test
        void shouldReturnExpensesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class), mock(DailyCashDto.class));

            when(expenseRepository.sumExpensesGroupedByDate(USER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.expensesGroupedByDate(USER_ID);

            assertSame(expected, result);
            verify(expenseRepository).sumExpensesGroupedByDate(USER_ID);
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            when(expenseRepository.sumExpensesGroupedByDate(USER_ID)).thenReturn(List.of());

            List<DailyCashDto> result = service.expensesGroupedByDate(USER_ID);

            assertEquals(List.of(), result);
            verify(expenseRepository).sumExpensesGroupedByDate(USER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(expenseRepository.sumExpensesGroupedByDate(USER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.expensesGroupedByDate(USER_ID);

            assertEquals(null, result);
            verify(expenseRepository).sumExpensesGroupedByDate(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.sumExpensesGroupedByDate(USER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.expensesGroupedByDate(USER_ID));
        }
    }

    @Nested
    class ExpensesAvgGroupedByDate {

        @Test
        void shouldReturnAverageExpensesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class));

            when(expenseRepository.avgExpensesGroupedByDate(USER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(USER_ID);

            assertSame(expected, result);
            verify(expenseRepository).avgExpensesGroupedByDate(USER_ID);
            verifyNoInteractions(revenueRepository, walletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            when(expenseRepository.avgExpensesGroupedByDate(USER_ID)).thenReturn(List.of());

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(USER_ID);

            assertEquals(List.of(), result);
            verify(expenseRepository).avgExpensesGroupedByDate(USER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(expenseRepository.avgExpensesGroupedByDate(USER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(USER_ID);

            assertEquals(null, result);
            verify(expenseRepository).avgExpensesGroupedByDate(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.avgExpensesGroupedByDate(USER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.expensesAvgGroupedByDate(USER_ID));
        }
    }

    @Nested
    class SumRevenues {

        @Test
        void shouldReturnSumRevenuesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("250.75");

            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(expected));

            BigDecimal result = service.sumRevenues(USER_ID, FROM, TO);

            assertEquals(expected, result);
            verify(revenueRepository).sumRevenuesByUserAndDateRange(USER_ID, FROM, TO);
            verifyNoInteractions(expenseRepository, walletRepository);
        }

        @Test
        void shouldReturnZeroWhenRepositoryReturnsEmptyOptional() {
            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.empty());

            BigDecimal result = service.sumRevenues(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result);
            verify(revenueRepository).sumRevenuesByUserAndDateRange(USER_ID, FROM, TO);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumRevenues(USER_ID, FROM, TO));
        }
    }

    @Nested
    class AvgRevenues {

        @Test
        void shouldReturnAverageRevenuesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("125.50");

            when(revenueRepository.avgRevenuesByUserIdAndPeriod(USER_ID, FROM, TO)).thenReturn(Optional.of(expected));

            BigDecimal result = service.avgRevenues(USER_ID, FROM, TO);

            assertEquals(expected, result);
            verify(revenueRepository).avgRevenuesByUserIdAndPeriod(USER_ID, FROM, TO);
            verifyNoInteractions(expenseRepository, walletRepository);
        }

        @Test
        void shouldReturnZeroWhenRepositoryReturnsEmptyOptional() {
            when(revenueRepository.avgRevenuesByUserIdAndPeriod(USER_ID, FROM, TO)).thenReturn(Optional.empty());

            BigDecimal result = service.avgRevenues(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result);
            verify(revenueRepository).avgRevenuesByUserIdAndPeriod(USER_ID, FROM, TO);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.avgRevenuesByUserIdAndPeriod(USER_ID, FROM, TO)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.avgRevenues(USER_ID, FROM, TO));
        }
    }

    @Nested
    class HighestRevenues {

        @Test
        void shouldReturnHighestRevenuesWhenRepositoryReturnsResults() {
            List<HighestRevenueDto> expected = List.of(mock(HighestRevenueDto.class), mock(HighestRevenueDto.class));

            when(revenueRepository.findHighestRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestRevenueDto> result = service.highestRevenues(USER_ID, FROM, TO, PAGE_SIZE);

            assertSame(expected, result);
            verify(revenueRepository).findHighestRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any());
            verifyNoInteractions(expenseRepository, walletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            List<HighestRevenueDto> expected = List.of();

            when(revenueRepository.findHighestRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestRevenueDto> result = service.highestRevenues(USER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(expected, result);
            verify(revenueRepository).findHighestRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any());
        }

        @Test
        void shouldAcceptPageSizeEqualToOne() {
            List<HighestRevenueDto> expected = List.of(mock(HighestRevenueDto.class));

            when(revenueRepository.findHighestRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenReturn(expected);

            List<HighestRevenueDto> result = service.highestRevenues(USER_ID, FROM, TO, 1);

            assertSame(expected, result);
            verify(revenueRepository).findHighestRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any());
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsZero() {
            assertThrows(IllegalArgumentException.class, () -> service.highestRevenues(USER_ID, FROM, TO, 0));
        }

        @Test
        void shouldThrowExceptionWhenPageSizeIsNegative() {
            assertThrows(IllegalArgumentException.class, () -> service.highestRevenues(USER_ID, FROM, TO, -1));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.findHighestRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.highestRevenues(USER_ID, FROM, TO, PAGE_SIZE));
        }
    }

    @Nested
    class RevenuesByCategory {

        @Test
        void shouldReturnSumOfRevenuesWhenRevenuesExist() {
            Revenue firstRevenue = mock(Revenue.class);
            Revenue secondRevenue = mock(Revenue.class);

            when(firstRevenue.getAmount()).thenReturn(new BigDecimal("100.50"));
            when(secondRevenue.getAmount()).thenReturn(new BigDecimal("200.25"));

            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of(firstRevenue, secondRevenue));

            BigDecimal result = service.revenuesByCategory(USER_ID, FROM, TO, RevenueCategory.SALARY);

            assertEquals(new BigDecimal("300.75"), result);
            verify(revenueRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY);
            verify(firstRevenue).getAmount();
            verify(secondRevenue).getAmount();
            verifyNoInteractions(expenseRepository, walletRepository);
        }

        @Test
        void shouldReturnZeroWhenNoRevenuesExist() {
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of());

            BigDecimal result = service.revenuesByCategory(USER_ID, FROM, TO, RevenueCategory.SALARY);

            assertEquals(BigDecimal.ZERO, result);
            verify(revenueRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY);
        }

        @Test
        void shouldReturnRevenueAmountWhenOneRevenueExists() {
            Revenue revenue = mock(Revenue.class);
            BigDecimal amount = new BigDecimal("999.99");

            when(revenue.getAmount()).thenReturn(amount);
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of(revenue));

            BigDecimal result = service.revenuesByCategory(USER_ID, FROM, TO, RevenueCategory.SALARY);

            assertEquals(amount, result);
            verify(revenue).getAmount();
        }

        @Test
        void shouldHandleNegativeRevenueAmountWhenRepositoryReturnsNegativeAmount() {
            Revenue revenue = mock(Revenue.class);
            BigDecimal amount = new BigDecimal("-10.50");

            when(revenue.getAmount()).thenReturn(amount);
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of(revenue));

            BigDecimal result = service.revenuesByCategory(USER_ID, FROM, TO, RevenueCategory.SALARY);

            assertEquals(amount, result);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.revenuesByCategory(USER_ID, FROM, TO, RevenueCategory.SALARY));
        }

        @Test
        void shouldThrowExceptionWhenRevenueAmountIsNull() {
            Revenue revenue = mock(Revenue.class);

            when(revenue.getAmount()).thenReturn(null);
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, FROM, TO, RevenueCategory.SALARY)).thenReturn(List.of(revenue));

            assertThrows(NullPointerException.class, () -> service.revenuesByCategory(USER_ID, FROM, TO, RevenueCategory.SALARY));
        }
    }

    @Nested
    class SumAllRevenues {

        @Test
        void shouldReturnSumAllRevenuesWhenRepositoryReturnsValue() {
            BigDecimal expected = new BigDecimal("800.00");

            when(revenueRepository.sumAllRevenuesByUserId(USER_ID)).thenReturn(expected);

            BigDecimal result = service.sumAllRevenues(USER_ID);

            assertEquals(expected, result);
            verify(revenueRepository).sumAllRevenuesByUserId(USER_ID);
            verifyNoInteractions(expenseRepository, walletRepository);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(revenueRepository.sumAllRevenuesByUserId(USER_ID)).thenReturn(null);

            BigDecimal result = service.sumAllRevenues(USER_ID);

            assertEquals(null, result);
            verify(revenueRepository).sumAllRevenuesByUserId(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.sumAllRevenuesByUserId(USER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.sumAllRevenues(USER_ID));
        }
    }

    @Nested
    class RevenuesGroupedByDate {

        @Test
        void shouldReturnRevenuesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class), mock(DailyCashDto.class));

            when(revenueRepository.sumRevenuesGroupedByDate(USER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.revenuesGroupedByDate(USER_ID);

            assertSame(expected, result);
            verify(revenueRepository).sumRevenuesGroupedByDate(USER_ID);
            verifyNoInteractions(expenseRepository, walletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            when(revenueRepository.sumRevenuesGroupedByDate(USER_ID)).thenReturn(List.of());

            List<DailyCashDto> result = service.revenuesGroupedByDate(USER_ID);

            assertEquals(List.of(), result);
            verify(revenueRepository).sumRevenuesGroupedByDate(USER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(revenueRepository.sumRevenuesGroupedByDate(USER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.revenuesGroupedByDate(USER_ID);

            assertEquals(null, result);
            verify(revenueRepository).sumRevenuesGroupedByDate(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.sumRevenuesGroupedByDate(USER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.revenuesGroupedByDate(USER_ID));
        }
    }

    @Nested
    class RevenuesAvgGroupedByDate {

        @Test
        void shouldReturnAverageRevenuesGroupedByDateWhenRepositoryReturnsResults() {
            List<DailyCashDto> expected = List.of(mock(DailyCashDto.class));

            when(revenueRepository.avgRevenuesGroupedByDate(USER_ID)).thenReturn(expected);

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(USER_ID);

            assertSame(expected, result);
            verify(revenueRepository).avgRevenuesGroupedByDate(USER_ID);
            verifyNoInteractions(expenseRepository, walletRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmptyList() {
            when(revenueRepository.avgRevenuesGroupedByDate(USER_ID)).thenReturn(List.of());

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(USER_ID);

            assertEquals(List.of(), result);
            verify(revenueRepository).avgRevenuesGroupedByDate(USER_ID);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsNull() {
            when(revenueRepository.avgRevenuesGroupedByDate(USER_ID)).thenReturn(null);

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(USER_ID);

            assertEquals(null, result);
            verify(revenueRepository).avgRevenuesGroupedByDate(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.avgRevenuesGroupedByDate(USER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.revenuesAvgGroupedByDate(USER_ID));
        }
    }

    @Nested
    class WalletBalance {

        @Test
        void shouldReturnWalletBalanceWhenWalletExists() {
            Wallet wallet = mock(Wallet.class);
            BigDecimal expected = new BigDecimal("1500.00");

            when(wallet.getBalance()).thenReturn(expected);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            BigDecimal result = service.walletBalance(USER_ID);

            assertEquals(expected, result);
            verify(walletRepository).findByUserId(USER_ID);
            verify(wallet).getBalance();
            verifyNoInteractions(expenseRepository, revenueRepository);
        }

        @Test
        void shouldReturnZeroWhenWalletDoesNotExist() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            BigDecimal result = service.walletBalance(USER_ID);

            assertEquals(BigDecimal.ZERO, result);
            verify(walletRepository).findByUserId(USER_ID);
            verifyNoInteractions(expenseRepository, revenueRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(walletRepository.findByUserId(USER_ID)).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class, () -> service.walletBalance(USER_ID));
        }
    }
}
