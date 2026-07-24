package com.finovara.reportservice.pdfexport.service.strategy.highest;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestexpense.service.HighestExpenseService;
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
class HighestExpensesPdfTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;

    @Mock
    private HighestExpenseService highestExpenseService;

    @Mock
    private PdfReportDocument document;

    private HighestExpensesPdf highestExpensesPdf;

    @BeforeEach
    void setUp() {
        highestExpensesPdf = new HighestExpensesPdf(highestExpenseService);
    }

    @Nested
    class GetType {

        @Test
        void shouldReturnHighestExpensesType() {
            assertThat(highestExpensesPdf.getType()).isEqualTo(PdfReportType.HIGHEST_EXPENSES);
        }
    }

    @Nested
    class GetTitle {

        @Test
        void shouldReturnConstantTitle() {
            assertThat(highestExpensesPdf.getTitle(PERIOD_TYPE)).isEqualTo("Największe wydatki");
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldContainBaseName() {
            assertThat(highestExpensesPdf.getFileName(PERIOD_TYPE)).contains("najwieksze-wydatki");
        }
    }

    @Nested
    class Generate {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldBuildReportFromHighestExpenseService(PeriodType periodType) throws Exception {
            HighestExpenseDto dto = new HighestExpenseDto(ExpenseCategory.FOOD, new BigDecimal("1000"));
            List<HighestExpenseDto> expenses = List.of(dto);

            when(highestExpenseService.getHighestExpense(USER_ID, periodType)).thenReturn(expenses);
            when(document.formatMoney(new BigDecimal("1000"))).thenReturn("1000");

            highestExpensesPdf.generate(document, USER_ID, periodType);

            verify(highestExpenseService).getHighestExpense(USER_ID, periodType);
            verify(document).addSection("Największe wydatki");
            verify(document).addInfo("Okres:", PdfReportText.periodLabel(periodType));
            verify(document).addBarChart(eq("Największe wydatki według kategorii"), any(), any(), eq(true));
            verify(document).addTable(eq(new String[]{"Kategoria", "Kwota"}), any());
        }

        @Test
        void shouldHandleEmptyExpenseList() throws Exception {
            when(highestExpenseService.getHighestExpense(USER_ID, PERIOD_TYPE)).thenReturn(List.of());

            highestExpensesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addSection("Największe wydatki");
            verify(document).addBarChart(any(), any(), any(), eq(true));
            verify(document).addTable(any(), any());
        }

        @Test
        void shouldHandleNullAmount() throws Exception {
            HighestExpenseDto dto = new HighestExpenseDto(ExpenseCategory.FOOD, null);
            when(highestExpenseService.getHighestExpense(USER_ID, PERIOD_TYPE)).thenReturn(List.of(dto));
            when(document.formatMoney(null)).thenReturn("0,00 PLN");

            highestExpensesPdf.generate(document, USER_ID, PERIOD_TYPE);

            verify(document).addBarChart(any(), any(), any(), eq(true));
            verify(document).addTable(any(), any());
        }
    }
}
