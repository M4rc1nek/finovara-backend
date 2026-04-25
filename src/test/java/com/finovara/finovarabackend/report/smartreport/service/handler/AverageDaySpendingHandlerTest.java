package com.finovara.finovarabackend.report.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
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
class AverageDaySpendingHandlerTest {
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

        LocalDate today = LocalDate.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(today.withDayOfMonth(1), today) + 1;
        BigDecimal expected = BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(days), java.math.RoundingMode.HALF_UP);

        when(expenseRepository.sumAllExpensesByUserAssignedId(userId)).thenReturn(BigDecimal.valueOf(100));
        when(templateService.getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING)).thenReturn("{amount}");

        String result = averageDaySpendingHandler.generate(userId);

        assertEquals(expected.toString(), result);

        verify(expenseRepository, times(1)).sumAllExpensesByUserAssignedId(userId);
        verify(templateService).getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING);
    }
}
