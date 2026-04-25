package com.finovara.finovarabackend.report.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseRateHandlerTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private SmartReportTemplateService templateService;
    @InjectMocks
    private ExpenseRateHandler expenseRateHandler;

    @Test
    void shouldReturnSmartReportType() {
        SmartReportType result = expenseRateHandler.getType();

        assertEquals(SmartReportType.EXPENSE_RATE, result);
    }

    @Test
    void shouldGenerateExpenseRateSuccessfully() {
        Long userId = 1L;

        when(expenseRepository.sumAllExpensesByUserAssignedId(anyLong())).thenReturn(BigDecimal.valueOf(50));

        when(revenueRepository.sumAllRevenuesByUserAssignedId(anyLong())).thenReturn(BigDecimal.valueOf(100));

        when(templateService.getRandomResponse(SmartReportType.EXPENSE_RATE)).thenReturn("{amount}");

        String result = expenseRateHandler.generate(userId);

        assertEquals(new BigDecimal("50.00"), new BigDecimal(result));

        verify(expenseRepository).sumAllExpensesByUserAssignedId(userId);
        verify(revenueRepository).sumAllRevenuesByUserAssignedId(userId);
        verify(templateService).getRandomResponse(SmartReportType.EXPENSE_RATE);
    }

    @Test
    void shouldReturnZeroWhenRevenueIsZero() {
        Long userId = 1L;

        when(expenseRepository.sumAllExpensesByUserAssignedId(anyLong()))
                .thenReturn(BigDecimal.valueOf(50));

        when(revenueRepository.sumAllRevenuesByUserAssignedId(anyLong()))
                .thenReturn(BigDecimal.ZERO);

        when(templateService.getRandomResponse(SmartReportType.EXPENSE_RATE))
                .thenReturn("{amount}");

        String result = expenseRateHandler.generate(userId);

        assertEquals(new BigDecimal("0.00"), new BigDecimal(result));
    }

}

