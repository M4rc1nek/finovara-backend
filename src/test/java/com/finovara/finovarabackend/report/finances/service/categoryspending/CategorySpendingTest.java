package com.finovara.finovarabackend.report.finances.service.categoryspending;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.categorypercentage.categoryspending.dto.CategorySpendingDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.categoryspending.service.ExpenseCategoryService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorySpendingTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ExpenseCategoryService expenseCategoryService;

    private String email;

    @BeforeEach
    void setUp() {
        email = "test@email.com";
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldGetCategorySpendingReport(PeriodType periodType) {
        User user = new User();
        user.setId(1L);

        BigDecimal summedExpenses = BigDecimal.valueOf(100);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(20));

        Expense expense2 = new Expense();
        expense2.setAmount(BigDecimal.valueOf(30));

        List<Expense> expenseCategory = List.of(expense, expense2);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.getExpensesSum(user.getId(), periodType)).thenReturn(summedExpenses);
        when(financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, ExpenseCategory.CLOTHING))
                .thenReturn(expenseCategory);

        CategorySpendingDto result = expenseCategoryService.getExpensePercentageByCategoryReport(email, ExpenseCategory.CLOTHING, periodType);

        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.category()).isEqualTo(ExpenseCategory.CLOTHING);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroPercentageWhenExpensesIsZero(PeriodType periodType) {
        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.getExpensesSum(user.getId(), periodType)).thenReturn(BigDecimal.ZERO);

        when(financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, ExpenseCategory.CLOTHING))
                .thenReturn(List.of());

        CategorySpendingDto result = expenseCategoryService.getExpensePercentageByCategoryReport(email, ExpenseCategory.CLOTHING, periodType);

        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                expenseCategoryService.getExpensePercentageByCategoryReport(email, ExpenseCategory.FOOD, PeriodType.MONTHLY));
    }

}
