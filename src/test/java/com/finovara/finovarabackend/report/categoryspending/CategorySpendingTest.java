package com.finovara.finovarabackend.report.categoryspending;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.dto.CategorySpendingDto;
import com.finovara.finovarabackend.report.finances.categoryspending.service.ReportsCategorySpendingService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorySpendingTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportsCategorySpendingService reportsCategorySpendingService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void shouldCalculatePercentageForCategory() {

        String email = "test@test.com";
        ExpenseCategory category = ExpenseCategory.FOOD;

        LocalDate today = LocalDate.of(2025, 3, 15);
        LocalDate startMonth = today.withDayOfMonth(1);

        Expense expense1 = new Expense();
        expense1.setAmount(BigDecimal.valueOf(100));

        Expense expense2 = new Expense();
        expense2.setAmount(BigDecimal.valueOf(50));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.today()).thenReturn(today);

        when(expenseRepository.findAllByUserAndCategoryAndDateRange(user.getId(), category, startMonth, today)).thenReturn(List.of(expense1, expense2));

        when(expenseRepository.sumExpensesByUserAndDateRange(user.getId(), startMonth, today)).thenReturn(BigDecimal.valueOf(500));

        CategorySpendingDto result = reportsCategorySpendingService.getCategorySpendingReport(email, category);

        assertThat(result.percentage()).isEqualByComparingTo("30.00");
        assertThat(result.category()).isEqualTo(category);
    }

    @Test
    void shouldReturnZeroPercentageWhenNoExpenses() {

        String email = "test@test.com";
        ExpenseCategory category = ExpenseCategory.FOOD;

        LocalDate today = LocalDate.of(2025, 3, 15);
        LocalDate startMonth = today.withDayOfMonth(1);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.today()).thenReturn(today);

        when(expenseRepository.findAllByUserAndCategoryAndDateRange(user.getId(), category, startMonth, today)).thenReturn(List.of());

        when(expenseRepository.sumExpensesByUserAndDateRange(user.getId(), startMonth, today)).thenReturn(BigDecimal.ZERO);

        CategorySpendingDto result = reportsCategorySpendingService.getCategorySpendingReport(email, category);

        assertThat(result.percentage()).isEqualByComparingTo("0");
    }

    @Test
    void shouldReturnZeroWhenCategoryHasNoExpensesButTotalExists() {

        String email = "test@test.com";
        ExpenseCategory category = ExpenseCategory.FOOD;

        LocalDate today = LocalDate.of(2025, 3, 15);
        LocalDate startMonth = today.withDayOfMonth(1);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.today()).thenReturn(today);

        when(expenseRepository.findAllByUserAndCategoryAndDateRange(user.getId(), category, startMonth, today)).thenReturn(List.of());
        when(expenseRepository.sumExpensesByUserAndDateRange(user.getId(), startMonth, today)).thenReturn(BigDecimal.valueOf(1000));

        CategorySpendingDto result = reportsCategorySpendingService.getCategorySpendingReport(email, category);

        assertThat(result.percentage()).isEqualByComparingTo("0");
    }
}