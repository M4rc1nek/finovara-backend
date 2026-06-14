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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseRateHandlerTest {

    private static final Long USER_ID = 1L;
    private static final String TEMPLATE = "Your expense rate is {amount}%";

    @Mock
    private FinanceBackendReportClient reportClient;
    @Mock
    private SmartReportTemplateService templateService;

    @InjectMocks
    private ExpenseRateHandler expenseRateHandler;

    @Nested
    class GetType {

        @Test
        void shouldReturnExpenseRateType() {
            assertThat(expenseRateHandler.getType()).isEqualTo(SmartReportType.EXPENSE_RATE);
        }
    }

    @Nested
    class Generate {

        @BeforeEach
        void setUp() {
            when(templateService.getRandomResponse(SmartReportType.EXPENSE_RATE)).thenReturn(TEMPLATE);
        }

        @Test
        void shouldCalculateExpenseRateAsPercentageOfRevenue() {
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.valueOf(50));
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.valueOf(100));

            String result = expenseRateHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", "50.00"));
            verify(reportClient).sumAllExpenses(USER_ID);
            verify(reportClient).sumAllRevenues(USER_ID);
        }

        @Test
        void shouldTreatNullAmountsAndZeroRevenueAsZeroRate() {
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(null);
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(null);

            String result = expenseRateHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", "0.00"));
        }
    }
}
