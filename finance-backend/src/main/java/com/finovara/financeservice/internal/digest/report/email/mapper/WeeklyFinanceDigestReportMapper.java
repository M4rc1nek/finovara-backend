package com.finovara.financeservice.internal.digest.report.email.mapper;

import com.finovara.contracts.notification.email.digest.report.finance.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.finance.WeeklyFinanceDigestReportDto;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class WeeklyFinanceDigestReportMapper {

    public WeeklyFinanceDigestReportDto toDto(Long userId, LocalDate from, LocalDate to,
                                       ExpenseSummary expenseSummary, RevenueSummary revenueSummary,
                                       PiggyBankSummaryDto piggyBankSummary) {
        BigDecimal savedMoney = calculateSavedMoney(expenseSummary.sum(), revenueSummary.sum());

        return new WeeklyFinanceDigestReportDto(
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
                revenueSummary.highestAmount(),
                revenueSummary.highestCategory(),
                revenueSummary.highestDate(),
                toPiggyBankSummaryDto(piggyBankSummary)
        );
    }

    private PiggyBankSummaryDto toPiggyBankSummaryDto(PiggyBankSummaryDto summary) {
        return new PiggyBankSummaryDto(
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
