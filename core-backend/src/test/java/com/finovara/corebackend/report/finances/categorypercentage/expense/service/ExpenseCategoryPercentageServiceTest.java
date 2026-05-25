package com.finovara.corebackend.report.finances.categorypercentage.expense.service;

import com.finovara.corebackend.expense.model.Expense;
import com.finovara.activityservice.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.corebackend.user.exception.notfound.UserNotFoundException;
import com.finovara.corebackend.user.model.User;
import com.finovara.activityservice.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import com.finovara.corebackend.util.user.service.UserManagerService;
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
class ExpenseCategoryPercentageServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ExpenseCategoryPercentageService expenseCategoryPercentageService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
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

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getExpensesSum(user.getId(), periodType)).thenReturn(summedExpenses);
        when(financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, ExpenseCategory.CLOTHING))
                .thenReturn(expenseCategory);

        ExpenseCategoryPercentageDto result = expenseCategoryPercentageService.getExpensePercentageByCategoryReport(userId, ExpenseCategory.CLOTHING, periodType);

        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.category()).isEqualTo(ExpenseCategory.CLOTHING);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroPercentageWhenExpensesIsZero(PeriodType periodType) {
        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getExpensesSum(user.getId(), periodType)).thenReturn(BigDecimal.ZERO);

        when(financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, ExpenseCategory.CLOTHING))
                .thenReturn(List.of());

        ExpenseCategoryPercentageDto result = expenseCategoryPercentageService.getExpensePercentageByCategoryReport(userId, ExpenseCategory.CLOTHING, periodType);

        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                expenseCategoryPercentageService.getExpensePercentageByCategoryReport(userId, ExpenseCategory.FOOD, PeriodType.MONTHLY));
    }

}
