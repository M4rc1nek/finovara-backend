package com.finovara.finovarabackend.expensehistory.service;

import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseHistoryTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseHistoryService expenseHistoryService;

    private User user;
    private String email;
    private Expense expense;
    private ExpenseDTO expenseDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        email = "test@email.com";

        expense = new Expense();
        expenseDTO = new ExpenseDTO(null, null, new BigDecimal(100),
                ExpenseCategory.FOOD, LocalDate.of(2026, 3, 12), "test");
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnMappedExpensesForEachPeriod(PeriodType periodType) {
        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.getExpensesInPeriodByCategory(1L, periodType, ExpenseCategory.FOOD)).thenReturn(List.of(expense));
        when(expenseMapper.mapExpenseToDTO(expense)).thenReturn(expenseDTO);

        List<ExpenseDTO> result = expenseHistoryService.getExpenseByCategory(email, periodType, ExpenseCategory.FOOD);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(expenseDTO);

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(financialPeriodService).getExpensesInPeriodByCategory(1L, periodType, ExpenseCategory.FOOD);
        verify(expenseMapper).mapExpenseToDTO(expense);
    }

    @Test
    void shouldReturnEmptyListWhenNoExpenses() {
        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.getExpensesInPeriodByCategory(1L, PeriodType.DAILY, ExpenseCategory.FOOD)).thenReturn(List.of());

        List<ExpenseDTO> result = expenseHistoryService.getExpenseByCategory(email, PeriodType.DAILY, ExpenseCategory.FOOD);

        assertThat(result).isEmpty();

        verify(expenseMapper, never()).mapExpenseToDTO(any());
    }

}