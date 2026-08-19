package com.finovara.financeservice.internal.digest.report.email.mapper;

import com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.WeeklyDigestReportDto;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DigestReportMapper {

    public WeeklyDigestReportDto toDto(Long userId, LocalDate from, LocalDate to,
                                       ExpenseSummary expenseSummary, RevenueSummary revenueSummary,
                                       PiggyBankSummaryDto piggyBankSummary) {
        BigDecimal savedMoney = calculateSavedMoney(expenseSummary.sum(), revenueSummary.sum());

        return new WeeklyDigestReportDto(
                userId,
                from,
                to,
                expenseSummary.sum(),
                expenseSummary.topCategory(),
                revenueSummary.sum(),
                revenueSummary.topCategory(),
                expenseSummary.remainingBudgetPercentage(),
                savedMoney,
                expenseSummary.daysWithoutExpense(),
                expenseSummary.highestAmount(),
                expenseSummary.highestCategory(),
                expenseSummary.highestDate(),
                toPiggyBankSummaryDto(piggyBankSummary)
        );
    }

    private com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto toPiggyBankSummaryDto(PiggyBankSummaryDto summary) {
        return new com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto(
                summary.quantityOfPiggyBanks(),
                summary.totalDepositedMoney(),
                summary.progressPercentage(),
                summary.remainingAmount(),
                summary.goalCompleted()
        );
    }

    private BigDecimal calculateSavedMoney(BigDecimal expensesSum, BigDecimal revenuesSum) {
        return revenuesSum.subtract(expensesSum);
    }
}