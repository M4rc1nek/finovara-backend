package com.finovara.finovarabackend.report.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthSpendingHandlerTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SmartReportTemplateService templateService;

    @InjectMocks
    private MonthSpendingHandler monthSpendingHandler;

    @Test
    void shouldReturnCorrectType() {
        assertEquals(SmartReportType.MONTH_SPENDING, monthSpendingHandler.getType());
    }

    @Test
    void shouldGenerateMonthlySpending() {
        Long userId = 1L;

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        when(expenseRepository.sumExpensesByUserAndDateRange(eq(userId), eq(startOfMonth), eq(today))).thenReturn(Optional.of(BigDecimal.valueOf(250)));

        when(templateService.getRandomResponse(SmartReportType.MONTH_SPENDING))
                .thenReturn("{amount}");

        String result = monthSpendingHandler.generate(userId);

        assertEquals("250", result);

        verify(expenseRepository).sumExpensesByUserAndDateRange(userId, startOfMonth, today);
        verify(templateService).getRandomResponse(SmartReportType.MONTH_SPENDING);
    }
}