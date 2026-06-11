package com.finovara.reportservice.pdfexport.service.strategy.percentage;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.reportservice.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
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
class ExpensePercentagePdfTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;
    private static final BigDecimal CATEGORY_PERCENTAGE = new BigDecimal("10");

    @Mock
    private ExpenseCategoryPercentageService expenseCategoryPercentageService;

    @Mock
    private PdfReportDocument document;

    private ExpensePercentagePdf expensePercentagePdf;

    @BeforeEach
    void setUp() {
        expensePercentagePdf = new ExpensePercentagePdf(expenseCategoryPercentageService);
    }

    @Nested
    class GetType {

        @Test
        void shouldReturnPercentageOfExpensesType() {
            assertThat(expensePercentagePdf.getType()).isEqualTo(PdfReportType.PERCENTAGE_OF_EXPENSES);
        }
    }

    @Nested
    class GetTitle {

        @Test
        void shouldReturnConstantTitle() {
            assertThat(expensePercentagePdf.getTitle(PERIOD_TYPE)).isEqualTo("Udział procentowy wydatków");
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldContainBaseName() {
            assertThat(expensePercentagePdf.getFileName(PERIOD_TYPE)).contains("udzial-procentowy-wydatkow");
        }
    }

    @Nested
    class Generate {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldBuildReportForEachPeriodType(PeriodType periodType) throws Exception {
            when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(
                    eq(USER_ID), any(ExpenseCategory.class), eq(periodType)))
                    .thenAnswer(invocation -> new ExpenseCategoryPercentageDto(
                            CATEGORY_PERCENTAGE, invocation.getArgument(1)));

            expensePercentagePdf.generate(document, USER_ID, periodType);

            verify(document).addSection("Udział wydatków według kategorii");
            verify(document).addInfo("Okres:", PdfReportText.periodLabel(periodType));
            verify(document).addPieChart(eq("Struktura wydatków"), any(), any());
            verify(document).addTable(eq(new String[]{"Kategoria", "Udział"}), any());

            for (ExpenseCategory category : ExpenseCategory.values()) {
                verify(expenseCategoryPercentageService)
                        .getExpensePercentageByCategoryReport(USER_ID, category, periodType);
            }
        }

        @Test
        void shouldHandleZeroAndNullPercentages() throws Exception {
            when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(
                    anyLong(), any(ExpenseCategory.class), eq(PERIOD_TYPE)))
                    .thenAnswer(invocation -> new ExpenseCategoryPercentageDto(null, invocation.getArgument(1)));

            expensePercentagePdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addPieChart(any(), any(), any());
            verify(document).addTable(any(), any());
        }
    }
}
