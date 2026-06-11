package com.finovara.corebackend.report.finances.chart.cashflow.service;

import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.report.finances.chart.builder.CashFlowChartService;
import com.finovara.corebackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.corebackend.report.finances.chart.dto.DailyCashDto;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotalCashFlowChartServiceTest {

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CashFlowChartService cashFlowChartService;

    @InjectMocks
    private TotalCashFlowChartService service;

    private Long userId;
    private List<DailyCashDto> expenses;
    private List<DailyCashDto> revenues;
    private List<CashFlowDto> resultDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        expenses = List.of(mock(DailyCashDto.class));
        revenues = List.of(mock(DailyCashDto.class));
        resultDto = List.of(mock(CashFlowDto.class));
    }

    @Test
    void shouldReturnCashFlowChart() {
        when(expenseRepository.sumExpensesGroupedByDate(userId)).thenReturn(expenses);
        when(revenueRepository.sumRevenuesGroupedByDate(userId)).thenReturn(revenues);
        when(cashFlowChartService.getCashFlowChart(expenses, revenues)).thenReturn(resultDto);

        List<CashFlowDto> result = service.getCashFlowChart(userId);

        assertEquals(resultDto, result);

        verify(expenseRepository).sumExpensesGroupedByDate(userId);
        verify(revenueRepository).sumRevenuesGroupedByDate(userId);
        verify(cashFlowChartService).getCashFlowChart(expenses, revenues);
    }

    @Test
    void shouldReturnEmptyResult() {
        List<DailyCashDto> emptyExpenses = List.of();
        List<DailyCashDto> emptyRevenues = List.of();
        List<CashFlowDto> emptyResult = List.of();

        when(expenseRepository.sumExpensesGroupedByDate(userId)).thenReturn(emptyExpenses);
        when(revenueRepository.sumRevenuesGroupedByDate(userId)).thenReturn(emptyRevenues);
        when(cashFlowChartService.getCashFlowChart(emptyExpenses, emptyRevenues)).thenReturn(emptyResult);

        List<CashFlowDto> result = service.getCashFlowChart(userId);

        assertEquals(emptyResult, result);

        verify(expenseRepository).sumExpensesGroupedByDate(userId);
        verify(revenueRepository).sumRevenuesGroupedByDate(userId);
        verify(cashFlowChartService).getCashFlowChart(emptyExpenses, emptyRevenues);
    }
}