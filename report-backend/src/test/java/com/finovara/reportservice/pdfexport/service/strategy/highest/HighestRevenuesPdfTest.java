package com.finovara.corebackend.pdfexport.report.service.strategy.highest;

import com.finovara.corebackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.corebackend.pdfexport.report.model.PdfReportType;
import com.finovara.corebackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.corebackend.report.finances.highesttransactions.highestrevenue.service.HighestRevenueService;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HighestRevenuesPdfTest {

    @Mock
    private HighestRevenueService highestRevenueService;

    @Mock
    private PdfReportDocument document;

    private HighestRevenuesPdf highestRevenuesPdfStrategy;

    @BeforeEach
    void setUp() {
        highestRevenuesPdfStrategy = new HighestRevenuesPdf(highestRevenueService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(highestRevenuesPdfStrategy.getType()).isEqualTo(PdfReportType.HIGHEST_REVENUES);
    }

    @Test
    void shouldReturnCorrectTitle() {
        assertThat(highestRevenuesPdfStrategy.getTitle(PeriodType.MONTHLY)).isEqualTo("Największe przychody");
    }

    @Test
    void shouldReturnCorrectFileName() {
        assertThat(highestRevenuesPdfStrategy.getFileName(PeriodType.MONTHLY)).contains("najwieksze-przychody");
    }

    @Test
    void shouldGenerateHappyPath() throws Exception {
        HighestRevenueDto dto1 = mock(HighestRevenueDto.class);
        HighestRevenueDto dto2 = mock(HighestRevenueDto.class);

        when(dto1.category()).thenReturn(RevenueCategory.SALARY);
        when(dto1.amount()).thenReturn(new BigDecimal("2000"));

        when(dto2.category()).thenReturn(RevenueCategory.BUSINESS);
        when(dto2.amount()).thenReturn(new BigDecimal("1500"));

        when(highestRevenueService.getHighestRevenue(1L, PeriodType.MONTHLY)).thenReturn(List.of(dto1, dto2));

        highestRevenuesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Największe przychody");
        verify(document).addInfo("Okres:", PdfReportText.periodLabel(PeriodType.MONTHLY));

        verify(document).addBarChart(eq("Największe przychody według kategorii"), any(), any(), eq(true));

        verify(document).addTable(eq(new String[]{"Kategoria", "Kwota"}), any());
    }

    @Test
    void shouldHandleEmptyList() throws Exception {
        when(highestRevenueService.getHighestRevenue(1L, PeriodType.MONTHLY)).thenReturn(List.of());

        highestRevenuesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Największe przychody");
        verify(document).addBarChart(any(), any(), any(), eq(true));
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldHandleNullAmounts() throws Exception {
        HighestRevenueDto dto = mock(HighestRevenueDto.class);

        when(dto.category()).thenReturn(RevenueCategory.SALARY);
        when(dto.amount()).thenReturn(null);

        when(highestRevenueService.getHighestRevenue(1L, PeriodType.MONTHLY)).thenReturn(List.of(dto));

        highestRevenuesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addBarChart(any(), any(), any(), eq(true));
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldCallServiceOnce() throws Exception {
        when(highestRevenueService.getHighestRevenue(anyLong(), any())).thenReturn(List.of());

        highestRevenuesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(highestRevenueService).getHighestRevenue(1L, PeriodType.MONTHLY);
    }
}