package com.finovara.reportservice.savingsplan.service.calculation;

import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.response.SavingsPlanReport;
import com.finovara.reportservice.savingsplan.dto.response.model.GoalResult;
import com.finovara.reportservice.savingsplan.mapper.SavingsPlanMapper;
import com.finovara.reportservice.savingsplan.model.amount.SavingsPlanAmounts;
import com.finovara.reportservice.savingsplan.model.history.SavingsPlanStats;
import com.finovara.reportservice.savingsplan.model.input.SavingsPlanInput;
import com.finovara.reportservice.savingsplan.model.result.SavingsPlanResult;
import com.finovara.reportservice.savingsplan.service.SavingsPlanMessages;
import com.finovara.reportservice.savingsplan.service.history.SavingsPlanStatsService;
import com.finovara.reportservice.savingsplan.service.money.MoneyAmountCalculator;
import com.finovara.reportservice.savingsplan.service.option.SavingsPlanOptionService;
import com.finovara.reportservice.savingsplan.service.validation.SavingsPlanValidator;
import com.finovara.reportservice.savingsplan.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsPlanCalculator {

    private final SavingsPlanStatsService savingsPlanStatsService;
    private final MoneyAmountCalculator moneyAmountCalculator;
    private final SavingsPlanOptionService savingsPlanOptionService;
    private final SavingsPlanMapper savingsPlanMapper;
    private final SavingsPlanValidator savingsPlanValidator;

    public SavingsPlanReport calculate(SavingsPlanRequest request, List<SavingsPlanMonthDto> monthlyData, LocalDate today) {
        savingsPlanValidator.validate(request, monthlyData, today);

        SavingsPlanStats stats = savingsPlanStatsService.calculateStats(monthlyData, request.revenueType());
        SavingsPlanAmounts amounts = moneyAmountCalculator.calculate(request, stats, today);

        SavingsPlanInput input = new SavingsPlanInput(request, stats, amounts);
        return savingsPlanMapper.toReport(request, stats, createResult(input));
    }

    private SavingsPlanResult createResult(SavingsPlanInput input) {
        if (input.amountStillNeededForGoal().signum() == 0) {
            return input.result(MoneyUtils.ZERO_MONEY, GoalResult.CAN_REACH_GOAL, List.of(), SavingsPlanMessages.GOAL_REACHED.text());
        }
        if (input.moneyNeededPerMonth().compareTo(input.moneyAvailablePerMonth()) <= 0) {
            return input.result(input.moneyNeededPerMonth(), GoalResult.CAN_REACH_GOAL, List.of(), SavingsPlanMessages.GOAL_WITHOUT_CUTS.text());
        }
        BigDecimal monthlySavings = input.moneyAvailablePerMonth().max(BigDecimal.ZERO);
        return input.result(monthlySavings, GoalResult.CANNOT_REACH_GOAL, savingsPlanOptionService.buildOptions(input), SavingsPlanMessages.GOAL_NOT_REACHED.text());
    }
}
