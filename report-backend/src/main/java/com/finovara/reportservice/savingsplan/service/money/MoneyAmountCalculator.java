package com.finovara.reportservice.savingsplan.service.money;

import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.model.amount.SavingsPlanAmounts;
import com.finovara.reportservice.savingsplan.model.history.SavingsPlanStats;
import com.finovara.reportservice.savingsplan.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Component
public class MoneyAmountCalculator {

    private static final BigDecimal DAYS_IN_MONTH = new BigDecimal("30");
    private static final int SHORT_TERM_DAYS = 60;

    public SavingsPlanAmounts calculate(SavingsPlanRequest request, SavingsPlanStats stats, LocalDate today) {
        BigDecimal amountStillNeededForGoal = amountStillNeededForGoal(request);
        long daysToTarget = ChronoUnit.DAYS.between(today, request.targetDate());
        int monthsToTarget = monthsToTarget(today, request.targetDate());

        BigDecimal moneyNeededPerMonth = moneyNeededPerMonth(amountStillNeededForGoal, monthsToTarget, daysToTarget);
        BigDecimal moneyAvailablePerMonth = MoneyUtils.round(stats.freeMoneyPerMonth().multiply(request.strategy().rate()));
        return new SavingsPlanAmounts(amountStillNeededForGoal, monthsToTarget, moneyNeededPerMonth, moneyAvailablePerMonth);
    }

    private BigDecimal amountStillNeededForGoal(SavingsPlanRequest request) {
        return MoneyUtils.round(request.targetAmount()
                .subtract(request.currentSavings())
                .max(BigDecimal.ZERO));
    }

    private int monthsToTarget(LocalDate today, LocalDate targetDate) {
        return Math.toIntExact(ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate)));
    }

    private BigDecimal moneyNeededPerMonth(BigDecimal amountStillNeededForGoal, int monthsToTarget, long daysToTarget) {
        if (amountStillNeededForGoal.signum() == 0) {
            return MoneyUtils.ZERO_MONEY;
        }
        if (daysToTarget <= SHORT_TERM_DAYS) {
            return MoneyUtils.round(amountStillNeededForGoal.multiply(DAYS_IN_MONTH).divide(BigDecimal.valueOf(daysToTarget), 2, RoundingMode.HALF_UP));
        }
        return MoneyUtils.round(amountStillNeededForGoal.divide(BigDecimal.valueOf(monthsToTarget), 2, RoundingMode.HALF_UP));
    }

}
