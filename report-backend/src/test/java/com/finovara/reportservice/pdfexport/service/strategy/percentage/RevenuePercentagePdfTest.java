package com.finovara.reportservice.pdfexport.service.strategy.percentage;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenuePercentagePdfTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;
    private static final BigDecimal CATEGORY_PERCENTAGE = new BigDecimal("10");

    @Mock
    private RevenueCategoryPercentageService revenueCategoryPercentageService;

    @Mock
    private PdfReportDocument document;

    private RevenuePercentagePdf revenuePercentagePdf;

    @BeforeEach
    void setUp() {
        revenuePercentagePdf = new RevenuePercentagePdf(revenueCategoryPercentageService);
    }

    @Nested
    class GetType {

        @Test
        void shouldReturnPercentageOfRevenuesType() {
            assertThat(revenuePercentagePdf.getType()).isEqualTo(PdfReportType.PERCENTAGE_OF_REVENUES);
        }
    }

    @Nested
    class GetTitle {

        @Test
        void shouldReturnConstantTitle() {
            assertThat(revenuePercentagePdf.getTitle(PERIOD_TYPE)).isEqualTo("Udział procentowy przychodów");
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldContainBaseName() {
            assertThat(revenuePercentagePdf.getFileName(PERIOD_TYPE)).contains("udzial-procentowy-przychodow");
        }
    }

    @Nested
    class Generate {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldBuildReportForEachPeriodType(PeriodType periodType) throws Exception {
            when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(
                    eq(USER_ID), any(RevenueCategory.class), eq(periodType)))
                    .thenAnswer(invocation -> new RevenueCategoryPercentageDto(
                            CATEGORY_PERCENTAGE, invocation.getArgument(1)));

            revenuePercentagePdf.generate(document, USER_ID, periodType);

            verify(document).addSection("Udział przychodów według kategorii");
            verify(document).addInfo("Okres:", PdfReportText.periodLabel(periodType));
            verify(document).addPieChart(eq("Struktura przychodów"), any(), any());
            verify(document).addTable(eq(new String[]{"Kategoria", "Udział"}), any());

            for (RevenueCategory category : RevenueCategory.values()) {
                verify(revenueCategoryPercentageService)
                        .getRevenuePercentageByCategoryReport(USER_ID, category, periodType);
            }
        }

        @Test
        void shouldHandleZeroAndNullPercentages() throws Exception {
            when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(
                    anyLong(), any(RevenueCategory.class), eq(PERIOD_TYPE)))
                    .thenAnswer(invocation -> new RevenueCategoryPercentageDto(null, invocation.getArgument(1)));

            revenuePercentagePdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addPieChart(any(), any(), any());
            verify(document).addTable(any(), any());
        }
    }
}
