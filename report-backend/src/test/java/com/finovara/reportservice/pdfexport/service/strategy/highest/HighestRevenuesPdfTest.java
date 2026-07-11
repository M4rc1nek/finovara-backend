package com.finovara.reportservice.pdfexport.service.strategy.highest;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestrevenue.service.HighestRevenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighestRevenuesPdfTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;

    @Mock
    private HighestRevenueService highestRevenueService;

    @Mock
    private PdfReportDocument document;

    private HighestRevenuesPdf highestRevenuesPdf;

    @BeforeEach
    void setUp() {
        highestRevenuesPdf = new HighestRevenuesPdf(highestRevenueService);
    }

    @Nested
    class GetType {

        @Test
        void shouldReturnHighestRevenuesType() {
            assertThat(highestRevenuesPdf.getType()).isEqualTo(PdfReportType.HIGHEST_REVENUES);
        }
    }

    @Nested
    class GetTitle {

        @Test
        void shouldReturnConstantTitle() {
            assertThat(highestRevenuesPdf.getTitle(PERIOD_TYPE)).isEqualTo("Największe przychody");
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldContainBaseName() {
            assertThat(highestRevenuesPdf.getFileName(PERIOD_TYPE)).contains("najwieksze-przychody");
        }
    }

    @Nested
    class Generate {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldBuildReportFromHighestRevenueService(PeriodType periodType) throws Exception {
            HighestRevenueDto dto = new HighestRevenueDto(RevenueCategory.SALARY, new BigDecimal("2000"));
            List<HighestRevenueDto> revenues = List.of(dto);

            when(highestRevenueService.getHighestRevenue(USER_ID, periodType)).thenReturn(revenues);
            when(document.formatMoney(new BigDecimal("2000"))).thenReturn("2000");

            highestRevenuesPdf.generate(document, USER_ID, periodType);

            verify(highestRevenueService).getHighestRevenue(USER_ID, periodType);
            verify(document).addSection("Największe przychody");
            verify(document).addInfo("Okres:", PdfReportText.periodLabel(periodType));
            verify(document).addBarChart(eq("Największe przychody według kategorii"), any(), any(), eq(true));
            verify(document).addTable(eq(new String[]{"Kategoria", "Kwota"}), any());
        }

        @Test
        void shouldHandleEmptyRevenueList() throws Exception {
            when(highestRevenueService.getHighestRevenue(USER_ID, PERIOD_TYPE)).thenReturn(List.of());

            highestRevenuesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addSection("Największe przychody");
            verify(document).addBarChart(any(), any(), any(), eq(true));
            verify(document).addTable(any(), any());
        }

        @Test
        void shouldHandleNullAmount() throws Exception {
            HighestRevenueDto dto = new HighestRevenueDto(RevenueCategory.SALARY, null);
            when(highestRevenueService.getHighestRevenue(USER_ID, PERIOD_TYPE)).thenReturn(List.of(dto));
            when(document.formatMoney(null)).thenReturn("0,00 PLN");

            highestRevenuesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addBarChart(any(), any(), any(), eq(true));
            verify(document).addTable(any(), any());
        }
    }
}
