package com.finovara.authbackend.expensehistory.service;

import com.finovara.authbackend.expense.dto.ExpenseDto;
import com.finovara.authbackend.expense.mapper.ExpenseMapper;
import com.finovara.authbackend.expense.model.Expense;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.authbackend.util.periodbalance.FinancialPeriodService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseHistoryTest {

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseHistoryService expenseHistoryService;

    private Long userId;
    private Expense expense;
    private ExpenseDto expenseDto;

    @BeforeEach
    void setUp() {
        userId = 1L;

        expense = new Expense();
        expenseDto = new ExpenseDto(null, null, new BigDecimal(100),
                ExpenseCategory.FOOD, LocalDate.of(2026, 3, 12), "test");
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnMappedExpensesForEachPeriod(PeriodType periodType) {
        when(financialPeriodService.getExpensesInPeriodByCategory(userId, periodType, ExpenseCategory.FOOD)).thenReturn(List.of(expense));
        when(expenseMapper.mapExpenseToDto(expense)).thenReturn(expenseDto);

        List<ExpenseDto> result = expenseHistoryService.getExpenseByCategory(userId, periodType, ExpenseCategory.FOOD);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(expenseDto);

        verify(financialPeriodService).getExpensesInPeriodByCategory(userId, periodType, ExpenseCategory.FOOD);
        verify(expenseMapper).mapExpenseToDto(expense);
    }

    @Test
    void shouldReturnEmptyListWhenNoExpenses() {
        when(financialPeriodService.getExpensesInPeriodByCategory(userId, PeriodType.DAILY, ExpenseCategory.FOOD)).thenReturn(List.of());

        List<ExpenseDto> result = expenseHistoryService.getExpenseByCategory(userId, PeriodType.DAILY, ExpenseCategory.FOOD);

        assertThat(result).isEmpty();
    }
}
