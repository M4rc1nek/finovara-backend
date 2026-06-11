package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.reportservice.feignclient.CoreBackendReportClient;
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
class SavingsRateHandlerTest {

    private static final Long USER_ID = 1L;
    private static final String TEMPLATE = "Your savings rate is {amount}%";

    @Mock
    private CoreBackendReportClient reportClient;
    @Mock
    private SmartReportTemplateService templateService;

    @InjectMocks
    private SavingsRateHandler savingsRateHandler;

    @Nested
    class GetType {

        @Test
        void shouldReturnSavingsRateType() {
            assertThat(savingsRateHandler.getType()).isEqualTo(SmartReportType.SAVINGS_RATE);
        }
    }

    @Nested
    class Generate {

        @BeforeEach
        void setUp() {
            when(templateService.getRandomResponse(SmartReportType.SAVINGS_RATE)).thenReturn(TEMPLATE);
        }

        @Test
        void shouldCalculateSavingsRateAsPercentageOfRevenue() {
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.valueOf(50));

            String result = savingsRateHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", "50.00"));
            verify(reportClient).sumAllRevenues(USER_ID);
            verify(reportClient).sumAllExpenses(USER_ID);
        }

        @Test
        void shouldTreatNullAmountsAndZeroRevenueAsZeroRate() {
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(null);
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(BigDecimal.valueOf(50));

            String result = savingsRateHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", "0"));
        }

        @Test
        void shouldReturnFullSavingsRateWhenExpensesAreZero() {
            when(reportClient.sumAllRevenues(USER_ID)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumAllExpenses(USER_ID)).thenReturn(null);

            String result = savingsRateHandler.generate(USER_ID);

            assertThat(result).isEqualTo(TEMPLATE.replace("{amount}", "100.00"));
        }
    }
}
