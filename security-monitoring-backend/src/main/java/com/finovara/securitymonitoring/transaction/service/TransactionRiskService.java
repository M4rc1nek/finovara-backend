package com.finovara.securitymonitoring.transaction.config;

import com.finovara.securitymonitoring.riskengine.model.RiskContext;
import com.finovara.securitymonitoring.riskengine.model.RiskRuleCode;
import com.finovara.securitymonitoring.riskengine.model.RiskTriggerType;
import com.finovara.securitymonitoring.riskengine.model.RiskType;
import com.finovara.securitymonitoring.riskengine.model.RuleResult;
import com.finovara.securitymonitoring.transaction.config.TransactionRiskProperties;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransactionRiskService {

    private final TransactionRiskProperties properties;

    public List<RuleResult> evaluate(RiskContext ctx) {
        TransactionProfile profile = ctx.transactionProfile();
        if (profile == null) {
            return List.of();
        }

        return Stream.of(
                        checkExpenseAboveAverage(ctx, profile),
                        checkExpenseNewAboveHighestRecord(ctx, profile),
                        checkFirstTransactionHighAmount(ctx, profile),
                        checkExpenseNewCategory(ctx, profile),
                        checkRevenueAboveAverage(ctx, profile),
                        checkRevenueBelowAverage(ctx, profile),
                        checkPiggyBankLargeDeposit(ctx, profile),
                        checkPiggyBankDepositDiffersFromLast(ctx, profile)
                )
                .flatMap(Optional::stream)
                .toList();
    }

    // 1. Wydatek znacznie powyzej sredniej
    private Optional<RuleResult> checkExpenseAboveAverage(RiskContext ctx, TransactionProfile profile) {
        boolean triggered = ctx.triggerType() == RiskTriggerType.EXPENSE
                && ctx.amount() != null
                && profile.getAverageExpenseAmount() != null
                && exceedsMultiplier(ctx.amount(), profile.getAverageExpenseAmount(), properties.expenseAboveAverageMultiplier());
        return ruleResultIf(triggered, RiskType.EXPENSE_ABOVE_AVERAGE);
    }

    // 2. Nowy rekord wydatku
    private Optional<RuleResult> checkExpenseNewAboveHighestRecord(RiskContext ctx, TransactionProfile profile) {
        boolean triggered = ctx.triggerType() == RiskTriggerType.EXPENSE
                && ctx.amount() != null
                && profile.getLargestExpenseAmount() != null
                && exceedsMultiplier(ctx.amount(), profile.getLargestExpenseAmount(), properties.expenseNewRecordMultiplier());
        return ruleResultIf(triggered, RiskType.EXPENSE_NEW_ABOVE_HIGHEST_RECORD);
    }

    // 3. Pierwsza transakcja danego typu, a kwota wysoka
    private Optional<RuleResult> checkFirstTransactionHighAmount(RiskContext ctx, TransactionProfile profile) {
        boolean isTrackedType = ctx.triggerType() == RiskTriggerType.EXPENSE || ctx.triggerType() == RiskTriggerType.REVENUE;
        if (!isTrackedType || ctx.amount() == null) {
            return Optional.empty();
        }

        long historyCount = ctx.triggerType() == RiskTriggerType.EXPENSE ? profile.getExpenseCount() : profile.getRevenueCount();
        boolean triggered = historyCount == 0 && ctx.amount().compareTo(properties.firstTransactionThreshold()) > 0;
        return ruleResultIf(triggered, RiskType.FIRST_TRANSACTION_HIGH_AMOUNT);
    }

    // 4. Nowa kategoria wydatku (inna niz ostatnio uzywana)
    private Optional<RuleResult> checkExpenseNewCategory(RiskContext ctx, TransactionProfile profile) {
        boolean triggered = ctx.triggerType() == RiskTriggerType.EXPENSE
                && ctx.category() != null
                && profile.getLastExpenseCategory() != null
                && !ctx.category().equals(profile.getLastExpenseCategory().name());
        return ruleResultIf(triggered, RiskType.EXPENSE_NEW_CATEGORY);
    }

    // 5. Przychod znacznie powyzej sredniej
    private Optional<RuleResult> checkRevenueAboveAverage(RiskContext ctx, TransactionProfile profile) {
        boolean triggered = ctx.triggerType() == RiskTriggerType.REVENUE
                && ctx.amount() != null
                && profile.getAverageRevenueAmount() != null
                && exceedsMultiplier(ctx.amount(), profile.getAverageRevenueAmount(), properties.revenueAboveAverageMultiplier());
        return ruleResultIf(triggered, RiskType.REVENUE_ABOVE_AVERAGE);
    }

    // 6. Przychod podejrzanie niski - typowe dla "transakcji testowej" skradzionych danych
    private Optional<RuleResult> checkRevenueBelowAverage(RiskContext ctx, TransactionProfile profile) {
        boolean triggered = ctx.triggerType() == RiskTriggerType.REVENUE
                && ctx.amount() != null
                && ctx.amount().compareTo(BigDecimal.ZERO) > 0
                && profile.getAverageRevenueAmount() != null
                && profile.getRevenueCount() > 0
                && belowRatio(ctx.amount(), profile.getAverageRevenueAmount(), properties.revenueBelowAverageRatio());
        return ruleResultIf(triggered, RiskType.REVENUE_BELOW_AVERAGE);
    }

    // 7. Duza wplata do skarbonki
    private Optional<RuleResult> checkPiggyBankLargeDeposit(RiskContext ctx, TransactionProfile profile) {
        boolean triggered = ctx.triggerType() == RiskTriggerType.PIGGY_BANK
                && ctx.amount() != null
                && profile.getLargestPiggyBankDeposit() != null
                && exceedsMultiplier(ctx.amount(), profile.getLargestPiggyBankDeposit(), properties.piggyBankLargeDepositMultiplier());
        return ruleResultIf(triggered, RiskType.PIGGY_BANK_LARGE_DEPOSIT);
    }

    // 8. Wplata mocno odbiegajaca (w gore lub w dol) od poprzedniej
    private Optional<RuleResult> checkPiggyBankDepositDiffersFromLast(RiskContext ctx, TransactionProfile profile) {
        BigDecimal lastDeposit = profile.getLastPiggyBankDepositAmount();
        boolean applicable = ctx.triggerType() == RiskTriggerType.PIGGY_BANK
                && ctx.amount() != null
                && lastDeposit != null
                && lastDeposit.compareTo(BigDecimal.ZERO) > 0;

        boolean triggered = applicable
                && (exceedsMultiplier(ctx.amount(), lastDeposit, properties.piggyBankDepositHigherMultiplier())
                || belowRatio(ctx.amount(), lastDeposit, properties.piggyBankDepositLowerRatio()));
        return ruleResultIf(triggered, RiskType.PIGGY_BANK_DEPOSIT_DIFFERS_FROM_LAST);
    }

    private Optional<RuleResult> ruleResultIf(boolean triggered, RiskType type) {
        return triggered ? Optional.of(new RuleResult(RiskRuleCode.of(type), type.points())) : Optional.empty();
    }

    private boolean exceedsMultiplier(BigDecimal value, BigDecimal base, BigDecimal multiplier) {
        return value.compareTo(base.multiply(multiplier)) > 0;
    }

    private boolean belowRatio(BigDecimal value, BigDecimal base, BigDecimal ratio) {
        return value.compareTo(base.multiply(ratio)) < 0;
    }
}