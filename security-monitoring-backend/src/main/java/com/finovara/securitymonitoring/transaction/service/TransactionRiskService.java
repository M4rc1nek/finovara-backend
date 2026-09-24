package com.finovara.securitymonitoring.transaction.service;

import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.dto.TriggeredRule;
import com.finovara.securitymonitoring.transaction.config.TransactionRiskProperties;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRiskService {

    private final TransactionRiskProperties properties;

    public List<TriggeredRule> evaluate(RiskContext context) {
        TransactionProfile profile = context.transactionProfile();
        if (profile == null) {
            return List.of();
        }

        log.info("Checking transaction risk for userId={}", context.userId());

        List<TriggeredRule> triggered = switch (context.triggerType()) {
            case EXPENSE -> evaluateExpenseRules(context, profile);
            case REVENUE -> evaluateRevenueRules(context, profile);
            case PIGGY_BANK -> evaluatePiggyBankRules(context, profile);
            default -> List.of();
        };

        log.info("Transaction risk points for userId={} is {}",
                context.userId(), triggered.stream().mapToInt(TriggeredRule::points).sum());

        return triggered;
    }

    private List<TriggeredRule> evaluateExpenseRules(RiskContext context, TransactionProfile profile) {
        BigDecimal amount = context.amount();
        List<TriggeredRule> rules = new ArrayList<>();

        if (amount != null) {
            if (amount.compareTo(properties.getExpenseHighAmount()) > 0) {
                rules.add(trigger(context, RiskRule.EXPENSE_HIGH, properties.getExpenseHighPoints()));
            }
            if (profile.getLargestExpenseAmount() != null && amount.compareTo(profile.getLargestExpenseAmount()) > 0) {
                rules.add(trigger(context, RiskRule.EXPENSE_RECORD, properties.getExpenseRecordPoints()));
            }
            if (profile.getExpenseCount() == 0 && amount.compareTo(properties.getFirstTransactionAmount()) > 0) {
                rules.add(trigger(context, RiskRule.FIRST_TRANSACTION_HIGH, properties.getFirstTransactionPoints()));
            }
        }

        if (context.category() != null && profile.getLastExpenseCategory() != null
                && !context.category().equals(profile.getLastExpenseCategory().name())) {
            rules.add(trigger(context, RiskRule.EXPENSE_NEW_CATEGORY, properties.getExpenseCategoryPoints()));
        }

        return rules;
    }

    private List<TriggeredRule> evaluateRevenueRules(RiskContext context, TransactionProfile profile) {
        BigDecimal amount = context.amount();
        if (amount == null) {
            return List.of();
        }
        List<TriggeredRule> rules = new ArrayList<>();

        if (amount.compareTo(properties.getRevenueHighAmount()) > 0) {
            rules.add(trigger(context, RiskRule.REVENUE_HIGH, properties.getRevenueHighPoints()));
        }

        if (amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(properties.getRevenueLowAmount()) < 0) {
            rules.add(trigger(context, RiskRule.REVENUE_LOW, properties.getRevenueLowPoints()));
        }

        if (profile.getRevenueCount() == 0 && amount.compareTo(properties.getFirstTransactionAmount()) > 0) {
            rules.add(trigger(context, RiskRule.FIRST_TRANSACTION_HIGH, properties.getFirstTransactionPoints()));
        }

        return rules;
    }

    private List<TriggeredRule> evaluatePiggyBankRules(RiskContext context, TransactionProfile profile) {
        BigDecimal amount = context.amount();
        if (amount == null) {
            return List.of();
        }
        List<TriggeredRule> rules = new ArrayList<>();

        if (amount.compareTo(properties.getPiggyBankHighAmount()) > 0) {
            rules.add(trigger(context, RiskRule.PIGGY_BANK_HIGH, properties.getPiggyBankHighPoints()));
        }

        BigDecimal lastDeposit = profile.getLastPiggyBankDepositAmount();
        if (lastDeposit != null && lastDeposit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = amount.subtract(lastDeposit).abs();
            if (diff.compareTo(properties.getPiggyBankDiffAmount()) > 0) {
                rules.add(trigger(context, RiskRule.PIGGY_BANK_DIFFERENT, properties.getPiggyBankDiffPoints()));
            }
        }

        return rules;
    }

    private TriggeredRule trigger(RiskContext context, RiskRule rule, int points) {
        log.info("Transaction Risk: Rule triggered for userId={}: {} (+{} points)", context.userId(), rule, points);
        return new TriggeredRule(rule, points);
    }
}