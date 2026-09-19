package com.finovara.securitymonitoring.transaction.service;

import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.securitymonitoring.riskengine.model.RiskRule;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import com.finovara.securitymonitoring.transaction.config.TransactionRiskProperties;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRiskService {

    private final TransactionRiskProperties properties;

    public int evaluate(RiskContext context) {
        TransactionProfile profile = context.transactionProfile();
        if (profile == null) {
            return 0;
        }

        log.info("Checking transaction risk for userId={}", context.userId());

        int totalPoints = addPoints(context, RiskRule.EXPENSE_HIGH, isExpenseHigh(context), properties.getExpenseHighPoints())
                + addPoints(context, RiskRule.EXPENSE_RECORD, isExpenseRecord(context, profile), properties.getExpenseRecordPoints())
                + addPoints(context, RiskRule.FIRST_TRANSACTION_HIGH, isFirstTransactionHigh(context, profile), properties.getFirstTransactionPoints())
                + addPoints(context, RiskRule.EXPENSE_NEW_CATEGORY, isExpenseNewCategory(context, profile), properties.getExpenseCategoryPoints())
                + addPoints(context, RiskRule.REVENUE_HIGH, isRevenueHigh(context), properties.getRevenueHighPoints())
                + addPoints(context, RiskRule.REVENUE_LOW, isRevenueLow(context), properties.getRevenueLowPoints())
                + addPoints(context, RiskRule.PIGGY_BANK_HIGH, isPiggyBankHigh(context), properties.getPiggyBankHighPoints())
                + addPoints(context, RiskRule.PIGGY_BANK_DIFFERENT, isPiggyBankDifferent(context, profile), properties.getPiggyBankDiffPoints());

        log.info("Transaction risk points for userId={} is {}", context.userId(), totalPoints);
        return totalPoints;
    }

    private boolean isExpenseHigh(RiskContext context) {
        return isExpense(context) && context.amount() != null
                && context.amount().compareTo(properties.getExpenseHighAmount()) > 0;
    }

    private boolean isExpenseRecord(RiskContext context, TransactionProfile profile) {
        return isExpense(context) && context.amount() != null && profile.getLargestExpenseAmount() != null
                && context.amount().compareTo(profile.getLargestExpenseAmount()) > 0;
    }

    private boolean isFirstTransactionHigh(RiskContext context, TransactionProfile profile) {
        boolean isTrackedType = isExpense(context) || isRevenue(context);
        if (!isTrackedType || context.amount() == null) {
            return false;
        }
        long historyCount = isExpense(context) ? profile.getExpenseCount() : profile.getRevenueCount();
        return historyCount == 0 && context.amount().compareTo(properties.getFirstTransactionAmount()) > 0;
    }

    private boolean isExpenseNewCategory(RiskContext context, TransactionProfile profile) {
        return isExpense(context) && context.category() != null && profile.getLastExpenseCategory() != null
                && !context.category().equals(profile.getLastExpenseCategory().name());
    }

    private boolean isRevenueHigh(RiskContext context) {
        return isRevenue(context) && context.amount() != null
                && context.amount().compareTo(properties.getRevenueHighAmount()) > 0;
    }

    private boolean isRevenueLow(RiskContext context) {
        return isRevenue(context) && context.amount() != null && context.amount().compareTo(BigDecimal.ZERO) > 0
                && context.amount().compareTo(properties.getRevenueLowAmount()) < 0;
    }

    private boolean isPiggyBankHigh(RiskContext context) {
        return isPiggyBank(context) && context.amount() != null
                && context.amount().compareTo(properties.getPiggyBankHighAmount()) > 0;
    }

    private boolean isPiggyBankDifferent(RiskContext context, TransactionProfile profile) {
        BigDecimal lastDeposit = profile.getLastPiggyBankDepositAmount();
        boolean applicable = isPiggyBank(context) && context.amount() != null
                && lastDeposit != null && lastDeposit.compareTo(BigDecimal.ZERO) > 0;
        if (!applicable) {
            return false;
        }
        BigDecimal diff = context.amount().subtract(lastDeposit).abs();
        return diff.compareTo(properties.getPiggyBankDiffAmount()) > 0;
    }

    private boolean isExpense(RiskContext context) {
        return context.triggerType() == RiskTriggerType.EXPENSE;
    }

    private boolean isRevenue(RiskContext context) {
        return context.triggerType() == RiskTriggerType.REVENUE;
    }

    private boolean isPiggyBank(RiskContext context) {
        return context.triggerType() == RiskTriggerType.PIGGY_BANK;
    }

    private int addPoints(RiskContext context, RiskRule riskRule, boolean triggered, int points) {
        if (triggered) {
            log.info("Transaction Risk: Rule triggered for userId={}: {} (+{} points)", context.userId(), riskRule, points);
        }
        return triggered ? points : 0;
    }
}