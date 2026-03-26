package com.finovara.finovarabackend.report.smartreport.service.handler.SavingsRate;


import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.handler.SavingsRateHandler;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsRateTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private SmartReportTemplateService templateService;
    @InjectMocks
    private SavingsRateHandler savingsRateHandler;

    @Test
    void shouldReturnCorrectType() {
        assertEquals(SmartReportType.SAVINGS_RATE, savingsRateHandler.getType());
    }

    @Test
    void shouldGenerateSavingsRateSuccessfully() {
        Long userId = 1L;

        when(revenueRepository.sumAllRevenuesByUserAssignedId(userId)).thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.sumAllExpensesByUserAssignedId(userId)).thenReturn(BigDecimal.valueOf(50));
        when(templateService.getRandomResponse(SmartReportType.SAVINGS_RATE)).thenReturn("{amount}");

        String result = savingsRateHandler.generate(userId);

        assertEquals(new BigDecimal("50.00"), new BigDecimal(result));

        verify(revenueRepository).sumAllRevenuesByUserAssignedId(userId);
        verify(expenseRepository).sumAllExpensesByUserAssignedId(userId);
        verify(templateService).getRandomResponse(SmartReportType.SAVINGS_RATE);
    }

    @Test
    void shouldReturnZeroWhenRevenueIsZero() {
        Long userId = 1L;

        when(revenueRepository.sumAllRevenuesByUserAssignedId(userId)).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumAllExpensesByUserAssignedId(userId)).thenReturn(BigDecimal.valueOf(50));
        when(templateService.getRandomResponse(SmartReportType.SAVINGS_RATE)).thenReturn("{amount}");

        String result = savingsRateHandler.generate(userId);

        assertEquals(new BigDecimal("0"), new BigDecimal(result));
    }
}