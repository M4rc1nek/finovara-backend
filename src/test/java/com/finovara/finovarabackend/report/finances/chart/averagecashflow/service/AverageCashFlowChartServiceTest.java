package com.finovara.finovarabackend.report.finances.chart.averagecashflow.service;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.chart.builder.CashFlowChartService;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AverageCashFlowChartServiceTest {

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CashFlowChartService cashFlowChartService;

    @InjectMocks
    private AverageCashFlowChartService service;

    @Test
    void shouldReturnCashFlowChartForUser() {
        Long userId = 1L;

        List<DailyCashDto> expenses = List.of(mock(DailyCashDto.class));
        List<DailyCashDto> revenues = List.of(mock(DailyCashDto.class));

        List<CashFlowDto> expectedResult = List.of(mock(CashFlowDto.class));

        when(expenseRepository.avgExpensesGroupedByDate(userId)).thenReturn(expenses);
        when(revenueRepository.avgRevenuesGroupedByDate(userId)).thenReturn(revenues);
        when(cashFlowChartService.getCashFlowChart(expenses, revenues)).thenReturn(expectedResult);

        List<CashFlowDto> result = service.getAverageCashFlowChart(userId);

        assertEquals(expectedResult, result);

        verify(expenseRepository, times(1)).avgExpensesGroupedByDate(userId);
        verify(revenueRepository, times(1)).avgRevenuesGroupedByDate(userId);
        verify(cashFlowChartService, times(1)).getCashFlowChart(expenses, revenues);
    }

    @Test
    void shouldHandleEmptyData() {
        Long userId = 2L;

        List<DailyCashDto> emptyExpenses = List.of();
        List<DailyCashDto> emptyRevenues = List.of();
        List<CashFlowDto> expectedResult = List.of();

        when(expenseRepository.avgExpensesGroupedByDate(userId)).thenReturn(emptyExpenses);
        when(revenueRepository.avgRevenuesGroupedByDate(userId)).thenReturn(emptyRevenues);
        when(cashFlowChartService.getCashFlowChart(emptyExpenses, emptyRevenues)).thenReturn(expectedResult);

        List<CashFlowDto> result = service.getAverageCashFlowChart(userId);

        assertEquals(expectedResult, result);

        verify(expenseRepository).avgExpensesGroupedByDate(userId);
        verify(revenueRepository).avgRevenuesGroupedByDate(userId);
        verify(cashFlowChartService).getCashFlowChart(emptyExpenses, emptyRevenues);
    }
}