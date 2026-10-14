package com.finovara.reportservice.pdfexport.strategy;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialCategoryPercentageDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.pdfexport.PdfReportContext;
import com.finovara.reportservice.pdfexport.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.strategy.label.PdfReportText;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class ImportantInformationPdf implements ReportPdfHandler {

    private static final String NO_DATA = "Brak danych";

    @Override
    public PdfReportType getType() {
        return PdfReportType.IMPORTANT_INFORMATION;
    }

    @Override
    public String getTitle(PeriodType periodType) {
        return "Najważniejsze informacje";
    }

    @Override
    public String getFileName(PeriodType periodType) {
        return "najwazniejsze-informacje.pdf";
    }

    @Override
    public void generate(PdfReportDocument document, PdfReportContext context) throws IOException {
        FinancialReportDto report = context.report(PeriodType.MONTHLY);

        BigDecimal walletBalance = context.walletBalance();
        BigDecimal monthlyRevenue = PdfReportValues.revenueFor(report, PeriodType.MONTHLY);
        BigDecimal monthlyExpense = PdfReportValues.expenseFor(report, PeriodType.MONTHLY);
        BigDecimal weeklyRevenue = PdfReportValues.revenueFor(report, PeriodType.WEEKLY);
        BigDecimal weeklyExpense = PdfReportValues.expenseFor(report, PeriodType.WEEKLY);
        BigDecimal dailyAverageRevenue = PdfReportValues.averagePerDay(monthlyRevenue, PeriodType.MONTHLY);
        BigDecimal dailyAverageExpense = PdfReportValues.averagePerDay(monthlyExpense, PeriodType.MONTHLY);

        List<FinancialCategoryPercentageDto> expensePercentages =
                PdfReportValues.safe(report.expenseCategoryPercentages());
        List<FinancialCategoryPercentageDto> revenuePercentages =
                PdfReportValues.safe(report.revenueCategoryPercentages());

        FinancialCategoryPercentageDto highestExpense = max(expensePercentages);
        FinancialCategoryPercentageDto lowestExpense = minPositive(expensePercentages);
        FinancialCategoryPercentageDto mainRevenueSource = max(revenuePercentages);

        BigDecimal spentPercentage = percentage(monthlyExpense, monthlyRevenue);
        BigDecimal savedPercentage = BigDecimal.valueOf(100).subtract(spentPercentage).max(BigDecimal.ZERO);

        document.addSection("Podstawowe informacje");
        document.addBarChart(
                "Przychody i wydatki w aktualnym okresie",
                List.of("Mies. przychody", "Mies. wydatki", "Tyg. przychody", "Tyg. wydatki"),
                List.of(monthlyRevenue, monthlyExpense, weeklyRevenue, weeklyExpense),
                true
        );
        document.addTable(
                new String[]{"Informacja", "Wartość"},
                List.of(
                        new String[]{"Saldo portfela", document.formatMoney(walletBalance)},
                        new String[]{"W tym miesiącu zarobiłeś", document.formatMoney(monthlyRevenue)},
                        new String[]{"W tym miesiącu wydałeś", document.formatMoney(monthlyExpense)},
                        new String[]{"W tym tygodniu zarobiłeś", document.formatMoney(weeklyRevenue)},
                        new String[]{"W tym tygodniu wydałeś", document.formatMoney(weeklyExpense)},
                        new String[]{"Średnie dzienne przychody", document.formatMoney(dailyAverageRevenue) + "/dzień"},
                        new String[]{"Średnie dzienne wydatki", document.formatMoney(dailyAverageExpense) + "/dzień"}
                )
        );

        document.addSection("Szczegółowe informacje");
        document.addPieChart(
                "Struktura wydatków w tym miesiącu",
                expensePercentages.stream().map(dto -> PdfReportText.expenseCategoryLabel(dto.category())).toList(),
                expensePercentages.stream().map(FinancialCategoryPercentageDto::percentage).toList()
        );
        document.addTable(
                new String[]{"Informacja", "Wartość"},
                List.of(
                        new String[]{"Z przychodów wydajesz", document.formatPercent(spentPercentage)},
                        new String[]{"Z przychodów oszczędzasz", document.formatPercent(savedPercentage)},
                        new String[]{"Główne źródło przychodu", revenueLabel(mainRevenueSource)},
                        new String[]{"Największy procent wydatków stanowi", expenseLabel(highestExpense)},
                        new String[]{"Najwięcej wydajesz na", expenseLabel(highestExpense)},
                        new String[]{"Najmniej wydajesz na", expenseLabel(lowestExpense)}
                )
        );
    }

    private FinancialCategoryPercentageDto max(List<FinancialCategoryPercentageDto> percentages) {
        return percentages.stream()
                .max(Comparator.comparing(FinancialCategoryPercentageDto::percentage))
                .orElse(null);
    }

    private FinancialCategoryPercentageDto minPositive(List<FinancialCategoryPercentageDto> percentages) {
        return percentages.stream()
                .filter(dto -> dto.percentage().signum() > 0)
                .min(Comparator.comparing(FinancialCategoryPercentageDto::percentage))
                .orElse(null);
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private String expenseLabel(FinancialCategoryPercentageDto dto) {
        if (dto == null || dto.percentage().signum() <= 0) {
            return NO_DATA;
        }
        return PdfReportText.expenseCategoryLabel(dto.category()) + " (" + dto.percentage() + "%)";
    }

    private String revenueLabel(FinancialCategoryPercentageDto dto) {
        if (dto == null || dto.percentage().signum() <= 0) {
            return NO_DATA;
        }
        return PdfReportText.revenueCategoryLabel(dto.category()) + " (" + dto.percentage() + "%)";
    }
}