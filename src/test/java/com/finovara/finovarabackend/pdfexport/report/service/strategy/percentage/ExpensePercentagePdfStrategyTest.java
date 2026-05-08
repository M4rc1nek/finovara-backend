package com.finovara.finovarabackend.pdfexport.report.service.strategy.percentage;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.finovarabackend.util.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePercentagePdfStrategyTest {

    @Mock
    private ExpenseCategoryPercentageService expenseCategoryPercentageService;

    @Mock
    private PdfReportDocument document;

    private ExpensePercentagePdfStrategy expensePercentagePdfStrategy;

    @BeforeEach
    void setUp() {
        expensePercentagePdfStrategy = new ExpensePercentagePdfStrategy(expenseCategoryPercentageService);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(expensePercentagePdfStrategy.getType()).isEqualTo(PdfReportType.PERCENTAGE_OF_EXPENSES);
    }

    @Test
    void shouldReturnCorrectTitle() {
        assertThat(expensePercentagePdfStrategy.getTitle(PeriodType.MONTHLY)).isEqualTo("Udział procentowy wydatków");
    }

    @Test
    void shouldReturnCorrectFileName() {
        assertThat(expensePercentagePdfStrategy.getFileName(PeriodType.MONTHLY)).contains("udzial-procentowy-wydatkow");
    }

    @Test
    void shouldGenerateHappyPath() throws Exception {
        when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(eq(1L), any(ExpenseCategory.class), eq(PeriodType.MONTHLY))).thenAnswer(invocation -> {
            ExpenseCategory category = invocation.getArgument(1);
            return new ExpenseCategoryPercentageDto(new BigDecimal("10"), category);
        });

        expensePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addSection("Udział wydatków według kategorii");
        verify(document).addInfo("Okres:", PdfReportText.periodLabel(PeriodType.MONTHLY));

        verify(document).addPieChart(eq("Struktura wydatków"), any(), any());

        verify(document).addTable(eq(new String[]{"Kategoria", "Udział"}), any());
    }

    @Test
    void shouldHandleZeroValues() throws Exception {
        when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(anyLong(), any(ExpenseCategory.class), any())).thenAnswer(invocation -> new ExpenseCategoryPercentageDto(BigDecimal.ZERO, invocation.getArgument(1)));

        expensePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addPieChart(any(), any(), any());
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldHandleNullValuesGracefully() throws Exception {
        when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(anyLong(), any(ExpenseCategory.class), any())).thenAnswer(invocation -> new ExpenseCategoryPercentageDto(null, invocation.getArgument(1)));

        expensePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        verify(document).addPieChart(any(), any(), any());
        verify(document).addTable(any(), any());
    }

    @Test
    void shouldCallServiceForAllExpenseCategories() throws Exception {
        when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(anyLong(), any(ExpenseCategory.class), any())).thenReturn(new ExpenseCategoryPercentageDto(BigDecimal.TEN, ExpenseCategory.FOOD));

        expensePercentagePdfStrategy.generate(document, 1L, PeriodType.MONTHLY);

        for (ExpenseCategory category : ExpenseCategory.values()) {
            verify(expenseCategoryPercentageService).getExpensePercentageByCategoryReport(1L, category, PeriodType.MONTHLY);
        }
    }
}