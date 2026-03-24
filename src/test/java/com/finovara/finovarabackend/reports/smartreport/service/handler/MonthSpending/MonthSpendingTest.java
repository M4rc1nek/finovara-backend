package com.finovara.finovarabackend.reports.smartreport.service.handler.MonthSpending;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.reports.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.reports.smartreport.service.handler.MonthSpendingHandler;
import com.finovara.finovarabackend.reports.smartreport.service.loader.SmartReportTemplateService;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthSpendingTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SmartReportTemplateService templateService;

    @Mock
    private SpentInPeriodService spentInPeriodService;

    @InjectMocks
    private MonthSpendingHandler monthSpendingHandler;

    @Test
    void shouldReturnCorrectType() {
        assertEquals(SmartReportType.MONTH_SPENDING, monthSpendingHandler.getType());
    }

    @Test
    void shouldGenerateMonthlySpending() {
        Long userId = 1L;

        LocalDate today = LocalDate.of(2024, 3, 15);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        when(spentInPeriodService.today()).thenReturn(today);

        when(expenseRepository.sumExpensesByUserAndDateRange(eq(userId), eq(startOfMonth), eq(today))).thenReturn(BigDecimal.valueOf(250));

        when(templateService.getRandomResponse(SmartReportType.MONTH_SPENDING))
                .thenReturn("{amount}");

        String result = monthSpendingHandler.generate(userId);

        assertEquals("250", result);

        verify(spentInPeriodService).today();
        verify(expenseRepository).sumExpensesByUserAndDateRange(userId, startOfMonth, today);
        verify(templateService).getRandomResponse(SmartReportType.MONTH_SPENDING);
    }
}