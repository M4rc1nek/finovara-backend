package com.finovara.finovarabackend.report.finances.service.average;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsAverageExpenseTest {

    @Mock
    private FinancialPeriodService financialPeriodService;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportAverageService reportAverageService;


    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldCalculateAverageExpense(PeriodType periodType){
        Long userId = 1L;
        List<Expense> expenses = List.of(new Expense(), new Expense());

        when(expenseRepository.findAllByUserAssignedId(userId)).thenReturn(expenses);
        when(financialPeriodService.getSpent(userId, periodType)).thenReturn(BigDecimal.valueOf(200));

        ReportDto result = reportAverageService.calculateAverageExpense(userId, periodType);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.periodType()).isEqualTo(periodType);
    }

    @Test
    void shouldReturnZeroWhenNoExpenses() {
        Long userId = 1L;

        when(expenseRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());
        when(financialPeriodService.getSpent(userId, PeriodType.DAILY)).thenReturn(BigDecimal.ZERO);

        ReportDto result = reportAverageService.calculateAverageExpense(userId, PeriodType.DAILY);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}