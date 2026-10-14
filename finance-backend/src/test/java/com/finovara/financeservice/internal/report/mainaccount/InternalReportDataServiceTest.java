package com.finovara.financeservice.internal.report.mainaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.internal.report.util.dto.RevenueCategorySumDto;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InternalReportDataServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);
    private static final int PAGE_SIZE = 5;
    private static final ExpenseCategory EXPENSE_CATEGORY_A = ExpenseCategory.values()[0];
    private static final ExpenseCategory EXPENSE_CATEGORY_B = ExpenseCategory.values()[1];
    private static final RevenueCategory REVENUE_CATEGORY_A = RevenueCategory.values()[0];
    private static final RevenueCategory REVENUE_CATEGORY_B = RevenueCategory.values()[1];
    private static final List<DailyCashDto> DAILY_CASH = List.of(
            new DailyCashDto(FROM, new BigDecimal("10.00")),
            new DailyCashDto(TO, new BigDecimal("20.00")));

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private HighestExpenseDto highestExpense;

    @Mock
    private HighestRevenueDto highestRevenue;

    @Mock
    private Wallet wallet;

    @InjectMocks
    private InternalReportDataService service;

    @Nested
    class HighestExpenses {

        @Test
        void shouldReturnHighestExpensesWhenRepositoryReturnsData() {
            List<HighestExpenseDto> expected = List.of(highestExpense);
            when(expenseRepository.findHighestExpensesByUserIdAndPeriod(USER_ID, FROM, TO, PageRequest.of(0, PAGE_SIZE)))
                    .thenReturn(expected);

            List<HighestExpenseDto> result = service.highestExpenses(USER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(expected, result);
            verify(expenseRepository).findHighestExpensesByUserIdAndPeriod(USER_ID, FROM, TO, PageRequest.of(0, PAGE_SIZE));
            verifyNoMoreInteractions(expenseRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoExpenses() {
            when(expenseRepository.findHighestExpensesByUserIdAndPeriod(USER_ID, FROM, TO, PageRequest.of(0, PAGE_SIZE)))
                    .thenReturn(emptyList());

            List<HighestExpenseDto> result = service.highestExpenses(USER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(emptyList(), result);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void shouldThrowExceptionWhenPageSizeIsLessThanOne(int pageSize) {
            assertThrows(IllegalArgumentException.class, () -> service.highestExpenses(USER_ID, FROM, TO, pageSize));

            verifyNoInteractions(expenseRepository);
        }
    }

    @Nested
    class HighestRevenues {

        @Test
        void shouldReturnHighestRevenuesWhenRepositoryReturnsData() {
            List<HighestRevenueDto> expected = List.of(highestRevenue);
            when(revenueRepository.findHighestRevenuesByUserIdAndPeriod(USER_ID, FROM, TO, PageRequest.of(0, PAGE_SIZE)))
                    .thenReturn(expected);

            List<HighestRevenueDto> result = service.highestRevenues(USER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(expected, result);
            verify(revenueRepository).findHighestRevenuesByUserIdAndPeriod(USER_ID, FROM, TO, PageRequest.of(0, PAGE_SIZE));
            verifyNoMoreInteractions(revenueRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoRevenues() {
            when(revenueRepository.findHighestRevenuesByUserIdAndPeriod(USER_ID, FROM, TO, PageRequest.of(0, PAGE_SIZE)))
                    .thenReturn(emptyList());

            List<HighestRevenueDto> result = service.highestRevenues(USER_ID, FROM, TO, PAGE_SIZE);

            assertEquals(emptyList(), result);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void shouldThrowExceptionWhenPageSizeIsLessThanOne(int pageSize) {
            assertThrows(IllegalArgumentException.class, () -> service.highestRevenues(USER_ID, FROM, TO, pageSize));

            verifyNoInteractions(revenueRepository);
        }
    }

    @Nested
    class ExpenseSumsByCategory {

        @Test
        void shouldReturnSumsMappedByCategoryWhenRepositoryReturnsSums() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(List.of(
                    new ExpenseCategorySumDto(EXPENSE_CATEGORY_A, new BigDecimal("10.00")),
                    new ExpenseCategorySumDto(EXPENSE_CATEGORY_B, new BigDecimal("20.00"))));

            Map<ExpenseCategory, BigDecimal> result = service.expenseSumsByCategory(USER_ID, FROM, TO);

            assertEquals(Map.of(EXPENSE_CATEGORY_A, new BigDecimal("10.00"), EXPENSE_CATEGORY_B, new BigDecimal("20.00")), result);
            verify(expenseRepository).sumExpensesGroupedByCategory(USER_ID, FROM, TO);
            verifyNoMoreInteractions(expenseRepository);
        }

        @Test
        void shouldReturnEmptyMapWhenRepositoryReturnsNoSums() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(emptyList());

            Map<ExpenseCategory, BigDecimal> result = service.expenseSumsByCategory(USER_ID, FROM, TO);

            assertEquals(emptyMap(), result);
        }

        @Test
        void shouldThrowExceptionWhenCategoriesAreDuplicated() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(List.of(
                    new ExpenseCategorySumDto(EXPENSE_CATEGORY_A, new BigDecimal("10.00")),
                    new ExpenseCategorySumDto(EXPENSE_CATEGORY_A, new BigDecimal("20.00"))));

            assertThrows(IllegalStateException.class, () -> service.expenseSumsByCategory(USER_ID, FROM, TO));
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(List.of(
                    new ExpenseCategorySumDto(EXPENSE_CATEGORY_A, null)));

            assertThrows(NullPointerException.class, () -> service.expenseSumsByCategory(USER_ID, FROM, TO));
        }
    }

    @Nested
    class RevenueSumsByCategory {

        @Test
        void shouldReturnSumsMappedByCategoryWhenRepositoryReturnsSums() {
            when(revenueRepository.sumRevenuesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(List.of(
                    new RevenueCategorySumDto(REVENUE_CATEGORY_A, new BigDecimal("10.00")),
                    new RevenueCategorySumDto(REVENUE_CATEGORY_B, new BigDecimal("20.00"))));

            Map<RevenueCategory, BigDecimal> result = service.revenueSumsByCategory(USER_ID, FROM, TO);

            assertEquals(Map.of(REVENUE_CATEGORY_A, new BigDecimal("10.00"), REVENUE_CATEGORY_B, new BigDecimal("20.00")), result);
            verify(revenueRepository).sumRevenuesGroupedByCategory(USER_ID, FROM, TO);
            verifyNoMoreInteractions(revenueRepository);
        }

        @Test
        void shouldReturnEmptyMapWhenRepositoryReturnsNoSums() {
            when(revenueRepository.sumRevenuesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(emptyList());

            Map<RevenueCategory, BigDecimal> result = service.revenueSumsByCategory(USER_ID, FROM, TO);

            assertEquals(emptyMap(), result);
        }

        @Test
        void shouldThrowExceptionWhenCategoriesAreDuplicated() {
            when(revenueRepository.sumRevenuesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(List.of(
                    new RevenueCategorySumDto(REVENUE_CATEGORY_A, new BigDecimal("10.00")),
                    new RevenueCategorySumDto(REVENUE_CATEGORY_A, new BigDecimal("20.00"))));

            assertThrows(IllegalStateException.class, () -> service.revenueSumsByCategory(USER_ID, FROM, TO));
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            when(revenueRepository.sumRevenuesGroupedByCategory(USER_ID, FROM, TO)).thenReturn(List.of(
                    new RevenueCategorySumDto(REVENUE_CATEGORY_A, null)));

            assertThrows(NullPointerException.class, () -> service.revenueSumsByCategory(USER_ID, FROM, TO));
        }
    }

    @Nested
    class ExpensesGroupedByDate {

        @Test
        void shouldReturnDailyExpensesWhenRepositoryReturnsData() {
            when(expenseRepository.sumExpensesGroupedByDate(USER_ID)).thenReturn(DAILY_CASH);

            List<DailyCashDto> result = service.expensesGroupedByDate(USER_ID);

            assertEquals(DAILY_CASH, result);
            verify(expenseRepository).sumExpensesGroupedByDate(USER_ID);
            verifyNoMoreInteractions(expenseRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoDailyExpenses() {
            when(expenseRepository.sumExpensesGroupedByDate(USER_ID)).thenReturn(emptyList());

            List<DailyCashDto> result = service.expensesGroupedByDate(USER_ID);

            assertEquals(emptyList(), result);
        }
    }

    @Nested
    class ExpensesAvgGroupedByDate {

        @Test
        void shouldReturnAverageDailyExpensesWhenRepositoryReturnsData() {
            when(expenseRepository.avgExpensesGroupedByDate(USER_ID)).thenReturn(DAILY_CASH);

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(USER_ID);

            assertEquals(DAILY_CASH, result);
            verify(expenseRepository).avgExpensesGroupedByDate(USER_ID);
            verifyNoMoreInteractions(expenseRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoAverageDailyExpenses() {
            when(expenseRepository.avgExpensesGroupedByDate(USER_ID)).thenReturn(emptyList());

            List<DailyCashDto> result = service.expensesAvgGroupedByDate(USER_ID);

            assertEquals(emptyList(), result);
        }
    }

    @Nested
    class RevenuesGroupedByDate {

        @Test
        void shouldReturnDailyRevenuesWhenRepositoryReturnsData() {
            when(revenueRepository.sumRevenuesGroupedByDate(USER_ID)).thenReturn(DAILY_CASH);

            List<DailyCashDto> result = service.revenuesGroupedByDate(USER_ID);

            assertEquals(DAILY_CASH, result);
            verify(revenueRepository).sumRevenuesGroupedByDate(USER_ID);
            verifyNoMoreInteractions(revenueRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoDailyRevenues() {
            when(revenueRepository.sumRevenuesGroupedByDate(USER_ID)).thenReturn(emptyList());

            List<DailyCashDto> result = service.revenuesGroupedByDate(USER_ID);

            assertEquals(emptyList(), result);
        }
    }

    @Nested
    class RevenuesAvgGroupedByDate {

        @Test
        void shouldReturnAverageDailyRevenuesWhenRepositoryReturnsData() {
            when(revenueRepository.avgRevenuesGroupedByDate(USER_ID)).thenReturn(DAILY_CASH);

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(USER_ID);

            assertEquals(DAILY_CASH, result);
            verify(revenueRepository).avgRevenuesGroupedByDate(USER_ID);
            verifyNoMoreInteractions(revenueRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoAverageDailyRevenues() {
            when(revenueRepository.avgRevenuesGroupedByDate(USER_ID)).thenReturn(emptyList());

            List<DailyCashDto> result = service.revenuesAvgGroupedByDate(USER_ID);

            assertEquals(emptyList(), result);
        }
    }

    @Nested
    class WalletBalance {

        @Test
        void shouldReturnBalanceWhenWalletExists() {
            BigDecimal balance = new BigDecimal("123.45");
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getBalance()).thenReturn(balance);

            BigDecimal result = service.walletBalance(USER_ID);

            assertEquals(balance, result);
            verify(walletRepository).findByUserId(USER_ID);
            verifyNoMoreInteractions(walletRepository);
        }

        @Test
        void shouldReturnZeroWhenWalletDoesNotExist() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            BigDecimal result = service.walletBalance(USER_ID);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void shouldReturnZeroWhenWalletBalanceIsNull() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getBalance()).thenReturn(null);

            BigDecimal result = service.walletBalance(USER_ID);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void shouldReturnNegativeBalanceWhenWalletBalanceIsNegative() {
            BigDecimal balance = new BigDecimal("-50.00");
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getBalance()).thenReturn(balance);

            BigDecimal result = service.walletBalance(USER_ID);

            assertEquals(balance, result);
        }
    }
}