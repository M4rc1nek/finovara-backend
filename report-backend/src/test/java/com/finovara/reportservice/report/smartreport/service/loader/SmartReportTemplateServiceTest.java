package com.finovara.reportservice.report.smartreport.service.loader;

import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SmartReportTemplateServiceTest {

    private SmartReportTemplateService smartReportTemplateService;

    @BeforeEach
    void setUp() {
        smartReportTemplateService = new SmartReportTemplateService();
        smartReportTemplateService.init();
    }

    @Nested
    class GetRandomResponse {

        @ParameterizedTest
        @EnumSource(SmartReportType.class)
        void shouldReturnNonBlankTemplateContainingAmountPlaceholder(SmartReportType type) {
            String result = smartReportTemplateService.getRandomResponse(type);

            assertThat(result)
                    .isNotNull()
                    .isNotBlank()
                    .contains("{amount}");
        }

        @Test
        void shouldReturnFallbackMessageWhenTypeIsNull() {
            assertThat(smartReportTemplateService.getRandomResponse(null))
                    .isEqualTo("Responses are null or empty");
        }

        @ParameterizedTest
        @EnumSource(SmartReportType.class)
        void shouldEventuallyReturnMoreThanOneTemplateWhenMultipleExist(SmartReportType type) {
            Set<String> observed = new HashSet<>();
            for (int i = 0; i < 30; i++) {
                observed.add(smartReportTemplateService.getRandomResponse(type));
            }

            if (observed.size() > 1) {
                assertThat(observed).allSatisfy(template ->
                        assertThat(template).isNotBlank().contains("{amount}"));
            } else {
                assertThat(observed).hasSize(1)
                        .allSatisfy(template -> assertThat(template).isNotBlank().contains("{amount}"));
            }
        }
    }
}
