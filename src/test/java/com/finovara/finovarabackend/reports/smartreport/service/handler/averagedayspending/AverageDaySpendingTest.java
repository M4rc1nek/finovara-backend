package com.finovara.finovarabackend.reports.smartreport.service.handler.averagedayspending;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.reports.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.reports.smartreport.service.handler.AverageDaySpendingHandler;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AverageDaySpendingTest {
    @Mock
    private SpentInPeriodService spentInPeriodService;
    @Mock
    private SmartReportTemplateService templateService;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private AverageDaySpendingHandler averageDaySpendingHandler;

    @Test
    void shouldReturnSmartReportType() {
        SmartReportType result = averageDaySpendingHandler.getType();

        assertEquals(SmartReportType.AVERAGE_DAY_SPENDING, result);
    }

    @Test
    void shouldGenerateSuccessfully() {
        Long userId = 1L;

        LocalDate today = LocalDate.of(2026, 3, 12);

        when(spentInPeriodService.today()).thenReturn(today);
        when(expenseRepository.sumAllExpensesByUserAssignedId(userId)).thenReturn(BigDecimal.valueOf(100));
        when(templateService.getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING)).thenReturn("{amount}");

        String result = averageDaySpendingHandler.generate(userId);

        assertEquals("8", result);

        verify(expenseRepository, times(1)).sumAllExpensesByUserAssignedId(userId);
        verify(templateService).getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING);
    }
}
