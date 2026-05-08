package com.finovara.finovarabackend.pdfexport.report.service.strategy.highest;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.finances.highesttransactions.highestexpense.dto.HighestExpenseDto;
import com.finovara.finovarabackend.report.finances.highesttransactions.highestexpense.service.HighestExpenseService;
import com.finovara.finovarabackend.util.model.PeriodType;
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
class HighestExpensesPdfStrategyTest {

    @Mock
    private HighestExpenseService highestExpenseService;

    @Mock
    private PdfReportDocument document;

    private HighestExpensesPdfStrategy highestExpensesPdfStrategy;

    @BeforeEach
    void setUp() {
        highestExpensesPdfStrategy = new HighestExpensesPdfStrategy(highestExpenseService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(highestExpensesPdfStrategy.getType()).isEqualTo(PdfReportType.HIGHEST_EXPENSES);
    }

    @Test
    void shouldReturnCorrectTitle() {
        assertThat(highestExpensesPdfStrategy.getTitle(PeriodType.MONTHLY)).isEqualTo("Największe wydatki");
    }

    @Test
    void shouldReturnCorrectFileName() {
        assertThat(highestExpensesPdfStrategy.getFileName(PeriodType.MONTHLY)).contains("najwieksze-wydatki");
    }

    @Test
    void shouldGenerateHappyPath() throws Exception {
        HighestExpenseDto dto1 = mock(HighestExpenseDto.class);
        HighestExpenseDto dto2 = mock(HighestExpenseDto.class);

        when(dto1.expenseCategory()).thenReturn(ExpenseCategory.FOOD);
        when(dto1.amount()).thenReturn(new BigDecimal("1000"));

        when(dto2.expenseCategory()).thenReturn(ExpenseCategory.TRANSPORT);
        when(dto2.amount()).thenReturn(new BigDecimal("500"));

        when(highestExpenseService.getHighestExpense(1L, PeriodType.MONTHLY)).thenReturn(List.of(dto1, dto2));

        highestExpensesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Największe wydatki");
        verify(document).addInfo("Okres:", PdfReportText.periodLabel(PeriodType.MONTHLY));

        verify(document).addBarChart(eq("Największe wydatki według kategorii"), any(), any(), eq(true));

        verify(document).addTable(eq(new String[]{"Kategoria", "Kwota"}), any());
    }

    @Test
    void shouldHandleEmptyList() throws Exception {
        when(highestExpenseService.getHighestExpense(1L, PeriodType.MONTHLY)).thenReturn(List.of());

        highestExpensesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Największe wydatki");
        verify(document).addBarChart(any(), any(), any(), eq(true));
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldHandleNullAmountGracefully() throws Exception {
        HighestExpenseDto dto = mock(HighestExpenseDto.class);

        when(dto.expenseCategory()).thenReturn(ExpenseCategory.FOOD);
        when(dto.amount()).thenReturn(null);

        when(highestExpenseService.getHighestExpense(1L, PeriodType.MONTHLY)).thenReturn(List.of(dto));

        highestExpensesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addBarChart(any(), any(), any(), eq(true));
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldCallServiceOnce() throws Exception {
        when(highestExpenseService.getHighestExpense(anyLong(), any())).thenReturn(List.of());

        highestExpensesPdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(highestExpenseService).getHighestExpense(1L, PeriodType.MONTHLY);
    }
}