package com.finovara.finovarabackend.pdfexport.report.service.strategy;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.pdfexport.report.service.ReportPdfHandler;
import com.finovara.finovarabackend.pdfexport.report.service.strategy.label.PdfReportText;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.service.ExpenseCategoryPercentageService;
import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.finovarabackend.report.finances.sum.service.ReportSummaryService;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ImportantInformationPdf implements ReportPdfHandler {
    private final WalletService walletService;
    private final ReportSummaryService reportSummaryService;
    private final ReportAverageService reportAverageService;
    private final ExpenseCategoryPercentageService expenseCategoryPercentageService;
    private final RevenueCategoryPercentageService revenueCategoryPercentageService;
    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

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
        BigDecimal walletBalance = walletService.getWalletForUser(userId).balance();
        BigDecimal monthlyRevenue = reportSummaryService.sumRevenue(userId, PeriodType.MONTHLY).amount();
        BigDecimal monthlyExpense = reportSummaryService.sumExpense(userId, PeriodType.MONTHLY).amount();
        BigDecimal weeklyRevenue = reportSummaryService.sumRevenue(userId, PeriodType.WEEKLY).amount();
        BigDecimal weeklyExpense = reportSummaryService.sumExpense(userId, PeriodType.WEEKLY).amount();
        BigDecimal dailyAverageRevenue = reportAverageService.calculateAverageRevenue(userId, PeriodType.DAILY).amount();
        BigDecimal dailyAverageExpense = reportAverageService.calculateAverageExpense(userId, PeriodType.DAILY).amount();

        List<ExpenseCategoryPercentageDto> expensePercentages = expensePercentages(userId);
        List<RevenueCategoryPercentageDto> revenuePercentages = revenuePercentages(userId);

        ExpenseCategoryPercentageDto highestExpensePercentage = maxExpensePercentage(expensePercentages);
        ExpenseCategoryPercentageDto lowestExpensePercentage = minPositiveExpensePercentage(expensePercentages);
        RevenueCategoryPercentageDto mainRevenueSource = maxRevenuePercentage(revenuePercentages);

        BigDecimal allExpenses = expenseRepository.sumAllExpensesByUserAssignedId(userId);
        BigDecimal allRevenues = revenueRepository.sumAllRevenuesByUserAssignedId(userId);
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

    private List<ExpenseCategoryPercentageDto> expensePercentages(Long userId) {
        return Arrays.stream(ExpenseCategory.values())
                .map(category -> expenseCategoryPercentageService.getExpensePercentageByCategoryReport(userId, category, PeriodType.MONTHLY))
                .toList();
    }

    private List<RevenueCategoryPercentageDto> revenuePercentages(Long userId) {
        return Arrays.stream(RevenueCategory.values())
                .map(category -> revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(userId, category, PeriodType.MONTHLY))
                .toList();
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
        return value.multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP);
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
