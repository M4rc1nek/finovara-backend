package com.finovara.reportservice.report.finances.calculate.avg;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.util.dto.ReportDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAverageServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinanceBackendReportClient reportClient;

    @InjectMocks
    private ReportAverageService reportAverageService;

    @Nested
    class CalculateAverageExpense {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnDto(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(50));

            ReportDto result = reportAverageService.calculateAverageExpense(USER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("50");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).avgExpenses(USER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportAverageService.calculateAverageExpense(USER_ID, PeriodType.MONTHLY);

            assertThat(result.amount()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class CalculateAverageRevenue {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnDto(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.avgRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(70));

            ReportDto result = reportAverageService.calculateAverageRevenue(USER_ID, periodType);

            assertThat(result.amount()).isEqualByComparingTo("70");
            assertThat(result.periodType()).isEqualTo(periodType);
            verify(reportClient).avgRevenues(USER_ID, from, to);
        }

        @Test
        void shouldReturnZeroAmountWhenClientReturnsZero() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(reportClient.avgRevenues(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            ReportDto result = reportAverageService.calculateAverageRevenue(USER_ID, PeriodType.MONTHLY);

            assertThat(result.amount()).isEqualByComparingTo("0");
        }
    }
}
