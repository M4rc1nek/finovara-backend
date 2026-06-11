package com.finovara.corebackend.report.smartreport.service.loader;

import com.finovara.corebackend.report.smartreport.model.SmartReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmartReportTemplateServiceTest {

    private SmartReportTemplateService service;

    @BeforeEach
    void setUp() {
        service = new SmartReportTemplateService();
        service.init();
    }

    @Nested
    class GetRandomResponse {

        @Test
        void shouldReturnResponseForValidType() {
            String result = service.getRandomResponse(SmartReportType.MONTH_SPENDING);

            assertThat(result).isNotNull();
            assertThat(result).isNotBlank();
        }

        @Test
        void shouldReturnResponseForEachType() {
            for (SmartReportType type : SmartReportType.values()) {
                String result = service.getRandomResponse(type);

                assertThat(result).isNotNull();
                assertThat(result).isNotBlank();
            }
        }

        @Test
        void shouldReturnFallbackWhenTypeNotLoaded() {
            String result = service.getRandomResponse(null);

            assertThat(result).isEqualTo("Responses are null or empty");
        }
    }

    @Nested
    class TemplatesLoading {
        @Test
        void shouldLoadTemplatesForMonthSpending() {
            String result = service.getRandomResponse(SmartReportType.MONTH_SPENDING);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldLoadTemplatesForAverageDaySpending() {
            String result = service.getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldLoadTemplatesForExpenseRate() {
            String result = service.getRandomResponse(SmartReportType.EXPENSE_RATE);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldLoadTemplatesForSavingsRate() {
            String result = service.getRandomResponse(SmartReportType.SAVINGS_RATE);

            assertThat(result).isNotNull();
        }
    }
}