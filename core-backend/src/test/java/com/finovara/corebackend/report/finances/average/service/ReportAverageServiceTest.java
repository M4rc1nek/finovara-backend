package com.finovara.corebackend.report.finances.average.service;

import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.activityservice.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class ReportAverageServiceTest {

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ReportAverageService reportAverageService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @Nested
    class ExpenseAverageTests {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldCalculateAverageExpense(PeriodType periodType) {

            when(financialPeriodService.getAverageExpense(userId, periodType)).thenReturn(BigDecimal.valueOf(50));

            ReportDto result = reportAverageService.calculateAverageExpense(userId, periodType);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(result.periodType()).isEqualTo(periodType);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnZeroWhenNoExpenses(PeriodType periodType) {

            when(financialPeriodService.getAverageExpense(userId, periodType)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportAverageService.calculateAverageExpense(userId, periodType);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class RevenueAverageTests {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldCalculateAverageRevenue(PeriodType periodType) {

            when(financialPeriodService.getAverageRevenue(userId, periodType)).thenReturn(BigDecimal.valueOf(70));

            ReportDto result = reportAverageService.calculateAverageRevenue(userId, periodType);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(70));
            assertThat(result.periodType()).isEqualTo(periodType);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnZeroWhenNoRevenue(PeriodType periodType) {

            when(financialPeriodService.getAverageRevenue(userId, periodType)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportAverageService.calculateAverageRevenue(userId, periodType);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}