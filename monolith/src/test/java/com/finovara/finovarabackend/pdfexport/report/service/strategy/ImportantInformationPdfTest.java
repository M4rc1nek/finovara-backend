package com.finovara.finovarabackend.pdfexport.report.service.strategy;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.finovarabackend.report.finances.sum.service.ReportSummaryService;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.wallet.dto.WalletDto;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportantInformationPdfTest {
    @Mock
    private WalletService walletService;
    @Mock
    private ReportSummaryService reportSummaryService;
    @Mock
    private ReportAverageService reportAverageService;
    @Mock
    private ExpenseCategoryPercentageService expenseCategoryPercentageService;
    @Mock
    private RevenueCategoryPercentageService revenueCategoryPercentageService;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private ImportantInformationPdf importantInformationPdf;

    private static final Long USER_ID = 1L;

    @Nested
    class MetadataTests {

        @Test
        void GetTypeReturnsImportantInformation() {
            assertThat(importantInformationPdf.getType()).isEqualTo(PdfReportType.IMPORTANT_INFORMATION);
        }

        @Test
        void GetTitleReturnsSameValueRegardlessOfPeriodType() {
            assertThat(importantInformationPdf.getTitle(PeriodType.DAILY)).isEqualTo("Najważniejsze informacje");
            assertThat(importantInformationPdf.getTitle(PeriodType.WEEKLY)).isEqualTo("Najważniejsze informacje");
            assertThat(importantInformationPdf.getTitle(PeriodType.MONTHLY)).isEqualTo("Najważniejsze informacje");
        }

        @Test
        void GetFileNameReturnsSameValueRegardlessOfPeriodType() {
            assertThat(importantInformationPdf.getFileName(PeriodType.DAILY)).isEqualTo("najwazniejsze-informacje.pdf");
            assertThat(importantInformationPdf.getFileName(PeriodType.WEEKLY)).isEqualTo("najwazniejsze-informacje.pdf");
            assertThat(importantInformationPdf.getFileName(PeriodType.MONTHLY)).isEqualTo("najwazniejsze-informacje.pdf");
        }
    }

    @Nested
    class GenerateTests {

        @Mock
        private PdfReportDocument document;

        @BeforeEach
        void SetupDefaultMocks() {
            when(walletService.getWalletForUser(USER_ID)).thenReturn(new WalletDto(1L, USER_ID, BigDecimal.valueOf(5000)));

            when(reportSummaryService.sumRevenue(USER_ID, PeriodType.MONTHLY)).thenReturn(new ReportDto(PeriodType.MONTHLY, BigDecimal.valueOf(3000)));
            when(reportSummaryService.sumExpense(USER_ID, PeriodType.MONTHLY)).thenReturn(new ReportDto(PeriodType.MONTHLY, BigDecimal.valueOf(1500)));
            when(reportSummaryService.sumRevenue(USER_ID, PeriodType.WEEKLY)).thenReturn(new ReportDto(PeriodType.WEEKLY, BigDecimal.valueOf(750)));
            when(reportSummaryService.sumExpense(USER_ID, PeriodType.WEEKLY)).thenReturn(new ReportDto(PeriodType.WEEKLY, BigDecimal.valueOf(300)));

            when(reportAverageService.calculateAverageRevenue(USER_ID, PeriodType.DAILY)).thenReturn(new ReportDto(PeriodType.DAILY,
                    BigDecimal.valueOf(100)));
            when(reportAverageService.calculateAverageExpense(USER_ID, PeriodType.DAILY)).thenReturn(new ReportDto(PeriodType.DAILY,
                    BigDecimal.valueOf(50)));

            when(expenseCategoryPercentageService.getExpensePercentageByCategoryReport(eq(USER_ID), any(ExpenseCategory.class),
                    eq(PeriodType.MONTHLY))).thenReturn(new ExpenseCategoryPercentageDto(BigDecimal.valueOf(10), ExpenseCategory.FOOD));

            when(revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(eq(USER_ID), any(RevenueCategory.class),
                    eq(PeriodType.MONTHLY))).thenReturn(new RevenueCategoryPercentageDto(BigDecimal.valueOf(10), RevenueCategory.SALARY));

            when(expenseRepository.sumAllExpensesByUserAssignedId(USER_ID)).thenReturn(BigDecimal.valueOf(10000));
            when(revenueRepository.sumAllRevenuesByUserAssignedId(USER_ID)).thenReturn(BigDecimal.valueOf(20000));

            when(document.formatMoney(any())).thenAnswer(invocation -> invocation.getArgument(0) + " PLN");
            when(document.formatPercent(any())).thenAnswer(invocation -> invocation.getArgument(0) + "%");
        }

        @Nested
        class DocumentStructureTests {
            @Test
            void GenerateAddsBothSectionsInCorrectOrder() throws IOException {
                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);

                InOrder inOrder = inOrder(document);
                inOrder.verify(document).addSection("Podstawowe informacje");
                inOrder.verify(document).addSection("Szczegółowe informacje");
            }

            @Test
            void GenerateAddsBarChartWithMonthlyAndWeeklyData() throws IOException {
                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);

                verify(document).addBarChart(
                        eq("Przychody i wydatki w aktualnym okresie"),
                        eq(List.of("Mies. przychody", "Mies. wydatki", "Tyg. przychody", "Tyg. wydatki")),
                        eq(List.of(
                                BigDecimal.valueOf(3000),
                                BigDecimal.valueOf(1500),
                                BigDecimal.valueOf(750),
                                BigDecimal.valueOf(300)
                        )),
                        eq(true));
            }

            @Test
            void GenerateAddsPieChartForMonthlyExpenseStructure() throws IOException {
                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);

                verify(document).addPieChart(eq("Struktura wydatków w tym miesiącu"), any(), any());
            }

            @Test
            void GenerateAddsTwoTables() throws IOException {
                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);

                verify(document, times(2)).addTable(any(), any());
            }
        }

        @Nested
        class PercentageCalculationTests {
            @Test
            void GenerateFormatsSpentAndSavedPercentageSymmetrically() throws IOException {
                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);

                verify(document, times(2)).formatPercent(new BigDecimal("50.00"));
            }

            @Test
            void GenerateWhenRevenuesAreZeroSpentPercentageIsZero() throws IOException {
                when(revenueRepository.sumAllRevenuesByUserAssignedId(USER_ID)).thenReturn(BigDecimal.ZERO);

                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);

                verify(document).formatPercent(BigDecimal.ZERO);
            }

            @Test
            void GenerateWhenExpensesExceedRevenuesSavedPercentageIsClampedToZero() throws IOException {
                when(expenseRepository.sumAllExpensesByUserAssignedId(USER_ID)).thenReturn(BigDecimal.valueOf(30000));
                when(revenueRepository.sumAllRevenuesByUserAssignedId(USER_ID)).thenReturn(BigDecimal.valueOf(10000));

                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);

                verify(document).formatPercent(BigDecimal.ZERO);
            }
        }

        @Nested
        class PeriodTypeIgnoredTests {
            @Test
            void GenerateProducesIdenticalDocumentStructureForAnyPeriodType() throws IOException {
                importantInformationPdf.generate(document, USER_ID, PeriodType.DAILY);
                importantInformationPdf.generate(document, USER_ID, PeriodType.WEEKLY);
                importantInformationPdf.generate(document, USER_ID, PeriodType.MONTHLY);

                verify(document, times(3)).addSection("Podstawowe informacje");
                verify(document, times(3)).addSection("Szczegółowe informacje");
                verify(document, times(3)).addBarChart(any(), any(), any(), anyBoolean());
                verify(document, times(6)).addTable(any(), any());
            }
        }
    }
}