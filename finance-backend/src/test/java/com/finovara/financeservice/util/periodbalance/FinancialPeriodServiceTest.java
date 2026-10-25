package com.finovara.financeservice.util.periodbalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 10;
    private static final PeriodType PERIOD = PeriodType.values()[0];
    private static final ExpenseCategory EXPENSE_CATEGORY = ExpenseCategory.values()[0];
    private static final RevenueCategory REVENUE_CATEGORY = RevenueCategory.values()[0];
    private static final BigDecimal SUM = new BigDecimal("123.45");

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private SharedExpenseRepository sharedExpenseRepository;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private Expense firstExpense;
    @Mock
    private Expense secondExpense;
    @Mock
    private Revenue firstRevenue;
    @Mock
    private Revenue secondRevenue;

    @InjectMocks
    private FinancialPeriodService service;

    private ArgumentCaptor<LocalDate> fromCaptor;
    private ArgumentCaptor<LocalDate> toCaptor;

    @BeforeEach
    void setUp() {
        setField(service, "pageSize", PAGE_SIZE);
        fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        toCaptor = ArgumentCaptor.forClass(LocalDate.class);
    }

    @Nested
    class GetExpensesInPeriodByCategory {

        @Test
        void shouldReturnExpensesFromRepositoryWhenExpensesExist() {
            List<Expense> expenses = List.of(firstExpense, secondExpense);
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class), any(PageRequest.class))).thenReturn(expenses);

            List<Expense> result = service.getExpensesInPeriodByCategory(USER_ID, PERIOD, EXPENSE_CATEGORY, 0);

            assertSame(expenses, result);
        }

        @Test
        void shouldReturnEmptyListWhenNoExpensesExist() {
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class), any(PageRequest.class))).thenReturn(List.of());

            List<Expense> result = service.getExpensesInPeriodByCategory(USER_ID, PERIOD, EXPENSE_CATEGORY, 0);

            assertEquals(List.of(), result);
        }

        @Test
        void shouldQueryFromPeriodStartDateToTodayWhenFetchingExpenses() {
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class), any(PageRequest.class))).thenReturn(List.of());

            service.getExpensesInPeriodByCategory(USER_ID, PERIOD, EXPENSE_CATEGORY, 0);

            verify(expenseRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), fromCaptor.capture(), toCaptor.capture(),
                    any(ExpenseCategory.class), any(PageRequest.class));
            assertEquals(LocalDate.now(), toCaptor.getValue());
            assertEquals(PERIOD.getStartDate(toCaptor.getValue()), fromCaptor.getValue());
        }

        @Test
        void shouldUseRequestedPageAndConfiguredPageSizeWhenFetchingExpenses() {
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class), any(PageRequest.class))).thenReturn(List.of());

            service.getExpensesInPeriodByCategory(USER_ID, PERIOD, EXPENSE_CATEGORY, 5);

            verify(expenseRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, PERIOD.getStartDate(LocalDate.now()), LocalDate.now(),
                    EXPENSE_CATEGORY, PageRequest.of(5, PAGE_SIZE));
        }

        @Test
        void shouldNotUseOtherRepositoriesWhenFetchingExpenses() {
            when(expenseRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class), any(PageRequest.class))).thenReturn(List.of());

            service.getExpensesInPeriodByCategory(USER_ID, PERIOD, EXPENSE_CATEGORY, 0);

            verifyNoInteractions(revenueRepository, sharedExpenseRepository);
        }
    }

    @Nested
    class GetRevenuesInPeriodByCategory {

        @Test
        void shouldReturnRevenuesFromRepositoryWhenRevenuesExist() {
            List<Revenue> revenues = List.of(firstRevenue, secondRevenue);
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(RevenueCategory.class), any(PageRequest.class))).thenReturn(revenues);

            List<Revenue> result = service.getRevenuesInPeriodByCategory(USER_ID, PERIOD, REVENUE_CATEGORY, 0);

            assertSame(revenues, result);
        }

        @Test
        void shouldReturnEmptyListWhenNoRevenuesExist() {
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(RevenueCategory.class), any(PageRequest.class))).thenReturn(List.of());

            List<Revenue> result = service.getRevenuesInPeriodByCategory(USER_ID, PERIOD, REVENUE_CATEGORY, 0);

            assertEquals(List.of(), result);
        }

        @Test
        void shouldQueryFromPeriodStartDateToTodayWhenFetchingRevenues() {
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(RevenueCategory.class), any(PageRequest.class))).thenReturn(List.of());

            service.getRevenuesInPeriodByCategory(USER_ID, PERIOD, REVENUE_CATEGORY, 0);

            verify(revenueRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), fromCaptor.capture(), toCaptor.capture(),
                    any(RevenueCategory.class), any(PageRequest.class));
            assertEquals(LocalDate.now(), toCaptor.getValue());
            assertEquals(PERIOD.getStartDate(toCaptor.getValue()), fromCaptor.getValue());
        }

        @Test
        void shouldUseRequestedPageAndConfiguredPageSizeWhenFetchingRevenues() {
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(RevenueCategory.class), any(PageRequest.class))).thenReturn(List.of());

            service.getRevenuesInPeriodByCategory(USER_ID, PERIOD, REVENUE_CATEGORY, 3);

            verify(revenueRepository).findAllByUserIdAndCreatedAtBetweenAndCategory(USER_ID, PERIOD.getStartDate(LocalDate.now()), LocalDate.now(),
                    REVENUE_CATEGORY, PageRequest.of(3, PAGE_SIZE));
        }

        @Test
        void shouldNotUseOtherRepositoriesWhenFetchingRevenues() {
            when(revenueRepository.findAllByUserIdAndCreatedAtBetweenAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(RevenueCategory.class), any(PageRequest.class))).thenReturn(List.of());

            service.getRevenuesInPeriodByCategory(USER_ID, PERIOD, REVENUE_CATEGORY, 0);

            verifyNoInteractions(expenseRepository, sharedExpenseRepository);
        }
    }

    @Nested
    class GetExpensesSum {

        @Test
        void shouldReturnSumWithoutCategoryWhenCategoryIsNull() {
            when(expenseRepository.sumExpensesByUserAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.of(SUM));

            BigDecimal result = service.getExpensesSum(USER_ID, PERIOD, null);

            assertEquals(SUM, result);
        }

        @Test
        void shouldReturnZeroWhenCategoryIsNullAndNoExpensesExist() {
            when(expenseRepository.sumExpensesByUserAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            BigDecimal result = service.getExpensesSum(USER_ID, PERIOD, null);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void shouldQueryWholePeriodWithoutCategoryWhenCategoryIsNull() {
            when(expenseRepository.sumExpensesByUserAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.of(SUM));

            service.getExpensesSum(USER_ID, PERIOD, null);

            verify(expenseRepository).sumExpensesByUserAndDateRange(anyLong(), fromCaptor.capture(), toCaptor.capture());
            assertEquals(LocalDate.now(), toCaptor.getValue());
            assertEquals(PERIOD.getStartDate(toCaptor.getValue()), fromCaptor.getValue());
            verifyNoMoreInteractions(expenseRepository);
        }

        @Test
        void shouldReturnSumForCategoryWhenCategoryIsProvided() {
            when(expenseRepository.sumExpensesByUserAndDateRangeAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class))).thenReturn(Optional.of(SUM));

            BigDecimal result = service.getExpensesSum(USER_ID, PERIOD, EXPENSE_CATEGORY);

            assertEquals(SUM, result);
        }

        @Test
        void shouldReturnZeroWhenCategoryIsProvidedAndNoExpensesExist() {
            when(expenseRepository.sumExpensesByUserAndDateRangeAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class))).thenReturn(Optional.empty());

            BigDecimal result = service.getExpensesSum(USER_ID, PERIOD, EXPENSE_CATEGORY);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void shouldQueryWholePeriodWithCategoryWhenCategoryIsProvided() {
            when(expenseRepository.sumExpensesByUserAndDateRangeAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class))).thenReturn(Optional.of(SUM));

            service.getExpensesSum(USER_ID, PERIOD, EXPENSE_CATEGORY);

            verify(expenseRepository).sumExpensesByUserAndDateRangeAndCategory(anyLong(), fromCaptor.capture(), toCaptor.capture(),
                    any(ExpenseCategory.class));
            assertEquals(LocalDate.now(), toCaptor.getValue());
            assertEquals(PERIOD.getStartDate(toCaptor.getValue()), fromCaptor.getValue());
            verifyNoMoreInteractions(expenseRepository);
        }

        @Test
        void shouldNotUseSharedRepositoryWhenCalculatingPersonalExpensesSum() {
            when(expenseRepository.sumExpensesByUserAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.of(SUM));

            service.getExpensesSum(USER_ID, PERIOD, null);

            verifyNoInteractions(sharedExpenseRepository, revenueRepository);
        }
    }

    @Nested
    class GetSharedExpensesSum {

        @Test
        void shouldReturnSumWithoutCategoryWhenCategoryIsNull() {
            when(sharedExpenseRepository.sumExpensesByUsersAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.of(SUM));

            BigDecimal result = service.getSharedExpensesSum(USER_ID, PERIOD, null);

            assertEquals(SUM, result);
        }

        @Test
        void shouldReturnZeroWhenCategoryIsNullAndNoSharedExpensesExist() {
            when(sharedExpenseRepository.sumExpensesByUsersAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            BigDecimal result = service.getSharedExpensesSum(USER_ID, PERIOD, null);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void shouldQueryWholePeriodWithoutCategoryWhenCategoryIsNull() {
            when(sharedExpenseRepository.sumExpensesByUsersAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.of(SUM));

            service.getSharedExpensesSum(USER_ID, PERIOD, null);

            verify(sharedExpenseRepository).sumExpensesByUsersAndDateRange(anyLong(), fromCaptor.capture(), toCaptor.capture());
            assertEquals(LocalDate.now(), toCaptor.getValue());
            assertEquals(PERIOD.getStartDate(toCaptor.getValue()), fromCaptor.getValue());
            verifyNoMoreInteractions(sharedExpenseRepository);
        }

        @Test
        void shouldReturnSumForCategoryWhenCategoryIsProvided() {
            when(sharedExpenseRepository.sumExpensesByUsersAndDateRangeAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class))).thenReturn(Optional.of(SUM));

            BigDecimal result = service.getSharedExpensesSum(USER_ID, PERIOD, EXPENSE_CATEGORY);

            assertEquals(SUM, result);
        }

        @Test
        void shouldReturnZeroWhenCategoryIsProvidedAndNoSharedExpensesExist() {
            when(sharedExpenseRepository.sumExpensesByUsersAndDateRangeAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class))).thenReturn(Optional.empty());

            BigDecimal result = service.getSharedExpensesSum(USER_ID, PERIOD, EXPENSE_CATEGORY);

            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        void shouldQueryWholePeriodWithCategoryWhenCategoryIsProvided() {
            when(sharedExpenseRepository.sumExpensesByUsersAndDateRangeAndCategory(anyLong(), any(LocalDate.class), any(LocalDate.class),
                    any(ExpenseCategory.class))).thenReturn(Optional.of(SUM));

            service.getSharedExpensesSum(USER_ID, PERIOD, EXPENSE_CATEGORY);

            verify(sharedExpenseRepository).sumExpensesByUsersAndDateRangeAndCategory(anyLong(), fromCaptor.capture(), toCaptor.capture(),
                    any(ExpenseCategory.class));
            assertEquals(LocalDate.now(), toCaptor.getValue());
            assertEquals(PERIOD.getStartDate(toCaptor.getValue()), fromCaptor.getValue());
            verifyNoMoreInteractions(sharedExpenseRepository);
        }

        @Test
        void shouldNotUsePersonalRepositoryWhenCalculatingSharedExpensesSum() {
            when(sharedExpenseRepository.sumExpensesByUsersAndDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Optional.of(SUM));

            service.getSharedExpensesSum(USER_ID, PERIOD, null);

            verifyNoInteractions(expenseRepository, revenueRepository);
        }
    }
}