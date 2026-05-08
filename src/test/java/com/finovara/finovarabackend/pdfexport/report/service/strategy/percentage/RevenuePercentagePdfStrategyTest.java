package com.finovara.finovarabackend.pdfexport.report.service.strategy.percentage;

import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.util.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenuePercentagePdfStrategyTest {

    @Mock
    private RevenueCategoryPercentageService revenueCategoryPercentageService;

    @Mock
    private PdfReportDocument document;

    private RevenuePercentagePdfStrategy revenuePercentagePdfStrategy;

    @BeforeEach
    void setUp() {
        revenuePercentagePdfStrategy = new RevenuePercentagePdfStrategy(revenueCategoryPercentageService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(revenuePercentagePdfStrategy.getType()).isEqualTo(PdfReportType.PERCENTAGE_OF_REVENUES);
    }

    @Test
    void shouldReturnCorrectTitle() {
        assertThat(revenuePercentagePdfStrategy.getTitle(PeriodType.MONTHLY)).isEqualTo("Udział procentowy przychodów");
    }

    @Test
    void shouldReturnCorrectFileName() {
        assertThat(revenuePercentagePdfStrategy.getFileName(PeriodType.MONTHLY)).contains("udzial-procentowy-przychodow");
    }

    @Test
    void shouldGenerateHappyPath() throws Exception {
        when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(eq(1L), any(RevenueCategory.class), eq(PeriodType.MONTHLY))).thenAnswer(invocation -> {
            RevenueCategory category = invocation.getArgument(1);
            return new RevenueCategoryPercentageDto(new BigDecimal("10"), category);
        });

        revenuePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Udział przychodów według kategorii");
        verify(document).addInfo("Okres:", PdfReportText.periodLabel(PeriodType.MONTHLY));

        verify(document).addPieChart(eq("Struktura przychodów"), any(), any());

        verify(document).addTable(eq(new String[]{"Kategoria", "Udział"}), any());
    }

    @Test
    void shouldHandleZeroPercentages() throws Exception {
        when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(eq(1L), any(RevenueCategory.class), eq(PeriodType.MONTHLY))).thenAnswer(invocation -> new RevenueCategoryPercentageDto(BigDecimal.ZERO, invocation.getArgument(1)));

        revenuePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addPieChart(any(), any(), any());
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldHandleNullPercentages() throws Exception {
        when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(eq(1L), any(RevenueCategory.class), eq(PeriodType.MONTHLY))).thenAnswer(invocation -> new RevenueCategoryPercentageDto(null, invocation.getArgument(1)));

        revenuePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addPieChart(any(), any(), any());
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldCallServiceForAllCategories() throws Exception {
        when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(anyLong(), any(RevenueCategory.class), any())).thenReturn(new RevenueCategoryPercentageDto(BigDecimal.TEN, RevenueCategory.SALARY));

        revenuePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        for (RevenueCategory category : RevenueCategory.values()) {
            verify(revenueCategoryPercentageService).getRevenuePercentageByCategoryReport(1L, category, PeriodType.MONTHLY);
        }
    }
}