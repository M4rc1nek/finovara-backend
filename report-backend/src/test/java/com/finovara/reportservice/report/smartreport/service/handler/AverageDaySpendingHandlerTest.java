package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AverageDaySpendingHandlerTest {

    private static final Long USER_ID = 1L;
    private static final String TEMPLATE = "Your average daily spending is {amount}";

    @Mock
    private FinanceBackendReportClient reportClient;
    @Mock
    private SmartReportTemplateService templateService;

    @InjectMocks
    private AverageDaySpendingHandler averageDaySpendingHandler;

    @Nested
    class GetType {

        @Test
        void shouldReturnAverageDaySpendingType() {
            assertThat(averageDaySpendingHandler.getType()).isEqualTo(SmartReportType.AVERAGE_DAY_SPENDING);
        }
    }

    @Nested
    class Generate {

        private LocalDate today;
        private LocalDate startOfMonth;

        @BeforeEach
        void setUp() {
            today = LocalDate.now();
            startOfMonth = today.withDayOfMonth(1);
            when(templateService.getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING)).thenReturn(TEMPLATE);
        }

        @Test
        void shouldReplaceAmountPlaceholderWithComputedAverage() {
            long days = ChronoUnit.DAYS.between(startOfMonth, today) + 1;
            BigDecimal sum = BigDecimal.valueOf(300);
            BigDecimal expectedAverage = sum.divide(BigDecimal.valueOf(days), RoundingMode.HALF_UP);

            when(reportClient.sumExpenses(USER_ID, startOfMonth, today)).thenReturn(sum);

            String result = averageDaySpendingHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", expectedAverage.toString()));
            verify(reportClient).sumExpenses(USER_ID, startOfMonth, today);
        }

        @Test
        void shouldApplyHalfUpRoundingWhenDivisionIsNotExact() {
            long days = ChronoUnit.DAYS.between(startOfMonth, today) + 1;
            BigDecimal sum = BigDecimal.valueOf(days * 3L + 1);
            BigDecimal expectedAverage = sum.divide(BigDecimal.valueOf(days), RoundingMode.HALF_UP);

            when(reportClient.sumExpenses(USER_ID, startOfMonth, today)).thenReturn(sum);

            String result = averageDaySpendingHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", expectedAverage.toString()));
        }

        @Test
        void shouldReturnZeroAverageWhenSumIsZero() {
            long days = ChronoUnit.DAYS.between(startOfMonth, today) + 1;
            BigDecimal expectedAverage = BigDecimal.ZERO.divide(BigDecimal.valueOf(days), RoundingMode.HALF_UP);

            when(reportClient.sumExpenses(USER_ID, startOfMonth, today)).thenReturn(BigDecimal.ZERO);

            String result = averageDaySpendingHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", expectedAverage.toString()));
        }
    }
}
