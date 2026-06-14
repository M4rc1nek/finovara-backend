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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthSpendingHandlerTest {

    private static final Long USER_ID = 1L;
    private static final String TEMPLATE = "You spent {amount} this month";

    @Mock
    private FinanceBackendReportClient reportClient;
    @Mock
    private SmartReportTemplateService templateService;

    @InjectMocks
    private MonthSpendingHandler monthSpendingHandler;

    @Nested
    class GetType {

        @Test
        void shouldReturnMonthSpendingType() {
            assertThat(monthSpendingHandler.getType()).isEqualTo(SmartReportType.MONTH_SPENDING);
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
            when(templateService.getRandomResponse(SmartReportType.MONTH_SPENDING)).thenReturn(TEMPLATE);
        }

        @Test
        void shouldReplaceAmountPlaceholderWithMonthlySum() {
            when(reportClient.sumExpenses(USER_ID, startOfMonth, today)).thenReturn(BigDecimal.valueOf(250));

            String result = monthSpendingHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", "250"));
            verify(reportClient).sumExpenses(USER_ID, startOfMonth, today);
        }

        @Test
        void shouldReturnZeroAmountWhenNoExpensesExist() {
            when(reportClient.sumExpenses(USER_ID, startOfMonth, today)).thenReturn(BigDecimal.ZERO);

            String result = monthSpendingHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", "0"));
        }
    }
}
