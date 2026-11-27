package com.finovara.reportservice.savingsplan.service.option;

import com.finovara.reportservice.savingsplan.dto.response.model.OptionType;
import com.finovara.reportservice.savingsplan.dto.response.model.PlanOption;
import com.finovara.reportservice.savingsplan.model.input.SavingsPlanInput;
import com.finovara.reportservice.savingsplan.service.SavingsPlanMessages;
import com.finovara.reportservice.savingsplan.util.MoneyUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class SavingsPlanOptionService {

    public List<PlanOption> buildOptions(SavingsPlanInput input) {
        List<PlanOption> options = new ArrayList<>();
        if (input.moneyAvailablePerMonth().signum() > 0) {
            options.add(buildLaterDateOption(input));
        }
        options.add(buildLowerGoalOption(input));
        return List.copyOf(options);
    }

    private PlanOption buildLaterDateOption(SavingsPlanInput input) {
        int months = countMonths(input.amountStillNeededForGoal(), input.moneyAvailablePerMonth());
        return new PlanOption(OptionType.LONGER_DEADLINE, input.request().targetAmount(), months, input.moneyAvailablePerMonth(), SavingsPlanMessages.LATER_DATE.text());
    }

    private PlanOption buildLowerGoalOption(SavingsPlanInput input) {
        BigDecimal monthlySavings = input.moneyAvailablePerMonth().max(BigDecimal.ZERO);
        BigDecimal targetAmount = MoneyUtils.round(input.request().currentSavings()
                .add(monthlySavings.multiply(BigDecimal.valueOf(input.monthsToTarget()))));
        return new PlanOption(OptionType.LOWER_GOAL, targetAmount, input.monthsToTarget(), monthlySavings, SavingsPlanMessages.LOWER_GOAL.text());
    }

    private int countMonths(BigDecimal moneyLeft, BigDecimal monthlySavings) {
        return moneyLeft.divide(monthlySavings, 0, RoundingMode.CEILING).intValueExact();
    }
}
