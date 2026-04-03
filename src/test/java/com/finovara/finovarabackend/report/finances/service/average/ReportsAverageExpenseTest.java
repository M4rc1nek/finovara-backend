package com.finovara.finovarabackend.report.finances.service.average;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsAverageExpenseTest {

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ReportAverageService reportAverageService;

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldCalculateAverageExpense(PeriodType periodType) {
        Long userId = 1L;

        when(financialPeriodService.getAverageExpense(userId, periodType)).thenReturn(BigDecimal.valueOf(50));

        ReportDto result = reportAverageService.calculateAverageExpense(userId, periodType);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.periodType()).isEqualTo(periodType);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroWhenNoExpenses(PeriodType periodType) {
        Long userId = 1L;

        when(financialPeriodService.getAverageExpense(userId, periodType)).thenReturn(BigDecimal.ZERO);

        ReportDto result = reportAverageService.calculateAverageExpense(userId, periodType);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}