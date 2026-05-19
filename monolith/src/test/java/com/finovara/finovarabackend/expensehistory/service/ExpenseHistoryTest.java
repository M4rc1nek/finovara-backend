package com.finovara.finovarabackend.expensehistory.service;

import com.finovara.finovarabackend.expense.dto.ExpenseDto;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseHistoryTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseHistoryService expenseHistoryService;

    private User user;
    private Long userId;
    private Expense expense;
    private ExpenseDto expenseDto;

    @BeforeEach
    void setUp() {
        user = new User();
        userId = 1L;
        user.setId(userId);

        expense = new Expense();
        expenseDto = new ExpenseDto(null, null, new BigDecimal(100),
                ExpenseCategory.FOOD, LocalDate.of(2026, 3, 12), "test");
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnMappedExpensesForEachPeriod(PeriodType periodType) {
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getExpensesInPeriodByCategory(1L, periodType, ExpenseCategory.FOOD)).thenReturn(List.of(expense));
        when(expenseMapper.mapExpenseToDto(expense)).thenReturn(expenseDto);

        List<ExpenseDto> result = expenseHistoryService.getExpenseByCategory(userId, periodType, ExpenseCategory.FOOD);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(expenseDto);

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(financialPeriodService).getExpensesInPeriodByCategory(1L, periodType, ExpenseCategory.FOOD);
        verify(expenseMapper).mapExpenseToDto(expense);
    }

    @Test
    void shouldReturnEmptyListWhenNoExpenses() {
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getExpensesInPeriodByCategory(1L, PeriodType.DAILY, ExpenseCategory.FOOD)).thenReturn(List.of());

        List<ExpenseDto> result = expenseHistoryService.getExpenseByCategory(userId, PeriodType.DAILY, ExpenseCategory.FOOD);

        assertThat(result).isEmpty();

        verify(expenseMapper, never()).mapExpenseToDto(any());
    }

}