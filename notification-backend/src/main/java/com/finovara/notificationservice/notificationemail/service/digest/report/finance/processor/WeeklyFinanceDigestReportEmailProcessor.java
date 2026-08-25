package com.finovara.notificationservice.notificationemail.service.digest.report.finance.processor;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.notification.email.digest.report.finance.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.finance.WeeklyFinanceDigestReportDto;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.feignclient.FinanceBackendClient;
import com.finovara.notificationservice.notificationemail.model.ScheduledEmailNotificationType;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import com.finovara.notificationservice.notificationemail.util.CategoryLabelResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyFinanceDigestReportEmailProcessor {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String NOT_AVAILABLE = "—";

    private final AuthBackendClient authBackendClient;
    private final FinanceBackendClient financeBackendClient;
    private final EmailNotifier emailNotifier;
    private final CategoryLabelResolver categoryLabelResolver;

    public void sendWeeklyFinanceDigestEmail() {
        List<WeeklyFinanceDigestReportDto> reports = financeBackendClient.getWeeklyFinanceDigestReports();
        reports.forEach(this::sendForUser);
    }

    private void sendForUser(WeeklyFinanceDigestReportDto report) {
        UserDataResponse user = authBackendClient.getUserData(report.userId());
        if (user.email().isEmpty()) {
            log.warn("Skipping digest email - no email found for userId={}", report.userId());
            return;
        }

        Map<String, String> placeholders = buildPlaceholders(report, user);
        emailNotifier.send(ScheduledEmailNotificationType.WEEKLY_FINANCE_DIGEST_REPORT_EMAIL, user.email().get(), placeholders);
    }

    private Map<String, String> buildPlaceholders(WeeklyFinanceDigestReportDto report, UserDataResponse user) {
        PiggyBankSummaryDto piggyBankSummary = report.piggyBankSummary();

        return Map.ofEntries(
                Map.entry("userName", user.username().orElse("Użytkowniku")),
                Map.entry("weekStart", formatDate(report.weekStart())),
                Map.entry("weekEnd", formatDate(report.weekEnd())),
                Map.entry("expensesSum", formatAmount(report.expensesSum())),
                Map.entry("topExpenseCategory", formatText(categoryLabelResolver.resolveExpenseCategoryName(report.topExpenseCategory()))),
                Map.entry("revenuesSum", formatAmount(report.revenuesSum())),
                Map.entry("topRevenueCategory", formatText(categoryLabelResolver.resolveRevenueCategoryName(report.topRevenueCategory()))),
                Map.entry("remainingBudgetPercentage", formatAmount(report.remainingBudgetPercentage())),
                Map.entry("savedMoney", formatAmount(report.savedMoney())),
                Map.entry("daysWithoutExpense", formatCount(report.daysWithoutExpense())),
                Map.entry("highestExpenseAmount", formatAmount(report.highestExpenseAmount())),
                Map.entry("highestExpenseCategory", formatText(categoryLabelResolver.resolveExpenseCategoryName(report.highestExpenseCategory()))),
                Map.entry("highestExpenseDate", formatDate(report.highestExpenseDate())),
                Map.entry("highestRevenueAmount", formatAmount(report.highestRevenueAmount())),
                Map.entry("highestRevenueCategory", formatText(categoryLabelResolver.resolveRevenueCategoryName(report.highestRevenueCategory()))),
                Map.entry("highestRevenueDate", formatDate(report.highestRevenueDate())),
                Map.entry("piggyBankQuantity", String.valueOf(piggyBankSummary.quantityOfPiggyBanks())),
                Map.entry("piggyBankTotalDeposited", formatAmount(piggyBankSummary.totalDepositedMoney())),
                Map.entry("piggyBankProgressPercentage", formatAmount(piggyBankSummary.progressPercentage())),
                Map.entry("piggyBankRemainingAmount", formatAmount(piggyBankSummary.remainingAmount())),
                Map.entry("piggyBankGoalCompleted", formatBoolean(piggyBankSummary.goalCompleted()))
        );
    }

    private String formatBoolean(boolean value) {
        return value ? "Tak" : "Nie";
    }

    private String formatText(String value) {
        return Optional.ofNullable(value).orElse(NOT_AVAILABLE);
    }

    private String formatAmount(BigDecimal value) {
        return Optional.ofNullable(value).map(BigDecimal::toPlainString).orElse(NOT_AVAILABLE);
    }

    private String formatCount(Integer value) {
        return Optional.ofNullable(value).map(String::valueOf).orElse(NOT_AVAILABLE);
    }

    private String formatDate(LocalDate value) {
        return Optional.ofNullable(value).map(DATE_FORMATTER::format).orElse(NOT_AVAILABLE);
    }
}
