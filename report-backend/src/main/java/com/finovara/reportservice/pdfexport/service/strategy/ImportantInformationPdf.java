package com.finovara.reportservice.pdfexport.service.strategy;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import com.finovara.reportservice.pdfexport.service.ReportPdfHandler;
import com.finovara.reportservice.pdfexport.service.strategy.label.PdfReportText;
import com.finovara.reportservice.report.finances.average.service.ReportAverageService;
import com.finovara.reportservice.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.reportservice.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.reportservice.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.reportservice.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.reportservice.report.finances.sum.service.ReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ImportantInformationPdf implements ReportPdfHandler {

    private final CoreBackendReportClient reportClient;
    private final ReportSummaryService reportSummaryService;
    private final ReportAverageService reportAverageService;
    private final ExpenseCategoryPercentageService expenseCategoryPercentageService;
    private final RevenueCategoryPercentageService revenueCategoryPercentageService;

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
    public void generate(PdfReportDocument document, Long userId, PeriodType periodType) throws IOException {
        BigDecimal walletBalance = reportClient.walletBalance(userId);
        BigDecimal monthlyRevenue = reportSummaryService.sumRevenue(userId, PeriodType.MONTHLY).amount();
        BigDecimal monthlyExpense = reportSummaryService.sumExpense(userId, PeriodType.MONTHLY).amount();
        BigDecimal weeklyRevenue = reportSummaryService.sumRevenue(userId, PeriodType.WEEKLY).amount();
        BigDecimal weeklyExpense = reportSummaryService.sumExpense(userId, PeriodType.WEEKLY).amount();
        BigDecimal dailyAverageRevenue = reportAverageService.calculateAverageRevenue(userId, PeriodType.DAILY).amount();
        BigDecimal dailyAverageExpense = reportAverageService.calculateAverageExpense(userId, PeriodType.DAILY).amount();

        List<ExpenseCategoryPercentageDto> expensePercentages = Arrays.stream(ExpenseCategory.values())
                .map(cat -> expenseCategoryPercentageService
                        .getExpensePercentageByCategoryReport(userId, cat, PeriodType.MONTHLY))
                .toList();

        List<RevenueCategoryPercentageDto> revenuePercentages = Arrays.stream(RevenueCategory.values())
                .map(cat -> revenueCategoryPercentageService
                        .getRevenuePercentageByCategoryReport(userId, cat, PeriodType.MONTHLY))
                .toList();

        ExpenseCategoryPercentageDto highestExpensePercentage = maxExpensePercentage(expensePercentages);
        ExpenseCategoryPercentageDto lowestExpensePercentage = minPositiveExpensePercentage(expensePercentages);
        RevenueCategoryPercentageDto mainRevenueSource = maxRevenuePercentage(revenuePercentages);

        BigDecimal allExpenses = reportClient.sumAllExpenses(userId);
        BigDecimal allRevenues = reportClient.sumAllRevenues(userId);
        BigDecimal spentPercentage = percentage(allExpenses, allRevenues);
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
                expensePercentages.stream()
                        .map(dto -> PdfReportText.expenseCategoryLabel(dto.category()))
                        .toList(),
                expensePercentages.stream()
                        .map(ExpenseCategoryPercentageDto::percentage)
                        .toList()
        );
        document.addTable(
                new String[]{"Informacja", "Wartość"},
                List.of(
                        new String[]{"Wszystkich swoich dochodów wydajesz", document.formatPercent(spentPercentage)},
                        new String[]{"Wszystkich swoich dochodów oszczędzasz", document.formatPercent(savedPercentage)},
                        new String[]{"Główne źródło przychodu", revenueLabel(mainRevenueSource)},
                        new String[]{"Największy procent wydatków stanowi", expenseLabel(highestExpensePercentage)},
                        new String[]{"Najwięcej wydajesz na", expenseLabel(highestExpensePercentage)},
                        new String[]{"Najmniej wydajesz na", expenseLabel(lowestExpensePercentage)}
                )
        );
    }

    private ExpenseCategoryPercentageDto maxExpensePercentage(List<ExpenseCategoryPercentageDto> percentages) {
        return percentages.stream()
                .max(Comparator.comparing(ExpenseCategoryPercentageDto::percentage))
                .orElse(null);
    }

    private ExpenseCategoryPercentageDto minPositiveExpensePercentage(List<ExpenseCategoryPercentageDto> percentages) {
        return percentages.stream()
                .filter(dto -> dto.percentage().compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.comparing(ExpenseCategoryPercentageDto::percentage))
                .orElse(null);
    }

    private RevenueCategoryPercentageDto maxRevenuePercentage(List<RevenueCategoryPercentageDto> percentages) {
        return percentages.stream()
                .max(Comparator.comparing(RevenueCategoryPercentageDto::percentage))
                .orElse(null);
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private String expenseLabel(ExpenseCategoryPercentageDto dto) {
        if (dto == null || dto.percentage().compareTo(BigDecimal.ZERO) <= 0) {
            return "Brak danych";
        }
        return PdfReportText.expenseCategoryLabel(dto.category()) + " (" + dto.percentage() + "%)";
    }

    private String revenueLabel(RevenueCategoryPercentageDto dto) {
        if (dto == null || dto.percentage().compareTo(BigDecimal.ZERO) <= 0) {
            return "Brak danych";
        }
        return PdfReportText.revenueCategoryLabel(dto.category()) + " (" + dto.percentage() + "%)";
    }
}