package com.finovara.reportservice.savingsplan.service;

import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.reportservice.savingsplan.dto.request.RevenueType;
import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.request.SavingsStrategy;
import com.finovara.reportservice.savingsplan.dto.response.SavingsPlanReport;
import com.finovara.reportservice.savingsplan.dto.response.model.GoalResult;
import com.finovara.reportservice.savingsplan.dto.response.model.OptionType;
import com.finovara.reportservice.savingsplan.mapper.SavingsPlanMapper;
import com.finovara.reportservice.savingsplan.service.calculation.SavingsPlanCalculator;
import com.finovara.reportservice.savingsplan.service.history.HistoryDataChecker;
import com.finovara.reportservice.savingsplan.service.history.SavingsPlanStatsService;
import com.finovara.reportservice.savingsplan.service.money.MoneyAmountCalculator;
import com.finovara.reportservice.savingsplan.service.money.NumberMath;
import com.finovara.reportservice.savingsplan.service.option.SavingsPlanOptionService;
import com.finovara.reportservice.savingsplan.service.validation.SavingsPlanValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsPlanBehaviorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 22);
    private final NumberMath numberMath = new NumberMath();
    private final SavingsPlanCalculator calculator = new SavingsPlanCalculator(
            new SavingsPlanStatsService(numberMath, new HistoryDataChecker(numberMath)),
            new MoneyAmountCalculator(),
            new SavingsPlanOptionService(),
            new SavingsPlanMapper(),
            new SavingsPlanValidator()
    );

    @Test
    void shouldReturnReachedGoalWithoutMonthlySaving() {
        SavingsPlanReport report = calculate("12000", "13000", "6000", "4800", RevenueType.STABLE, SavingsStrategy.BALANCED);

        assertThat(report.result()).isEqualTo(GoalResult.CAN_REACH_GOAL);
        assertThat(report.savingsPerMonth()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReachGoalWhenAvailableMoneyIsEnough() {
        SavingsPlanReport report = calculate("8000", "2000", "6000", "4800", RevenueType.STABLE, SavingsStrategy.BALANCED);

        assertThat(report.result()).isEqualTo(GoalResult.CAN_REACH_GOAL);
        assertThat(report.neededPerMonth()).isEqualByComparingTo("750.00");
    }

    @Test
    void shouldReturnTwoOptionsWhenGoalCannotBeReached() {
        SavingsPlanReport report = calculate("12000", "2000", "6000", "5500", RevenueType.STABLE, SavingsStrategy.BALANCED);

        assertThat(report.result()).isEqualTo(GoalResult.CANNOT_REACH_GOAL);
        assertThat(report.options()).extracting("type")
                .containsExactly(OptionType.LONGER_DEADLINE, OptionType.LOWER_GOAL);
    }

    @Test
    void shouldNotBuildLongerDateOptionWhenAvailableMoneyIsZeroOrBelow() {
        SavingsPlanReport zero = calculate("12000", "2000", "4800", "4800", RevenueType.STABLE, SavingsStrategy.BALANCED);
        SavingsPlanReport belowZero = calculate("12000", "2000", "4000", "4800", RevenueType.STABLE, SavingsStrategy.BALANCED);

        assertThat(zero.availablePerMonth()).isEqualByComparingTo("0.00");
        assertThat(belowZero.availablePerMonth()).isNegative();
        assertThat(zero.options()).extracting("type").doesNotContain(OptionType.LONGER_DEADLINE);
        assertThat(belowZero.options()).extracting("type").doesNotContain(OptionType.LONGER_DEADLINE);
    }

    @Test
    void shouldUseDifferentRevenueForStableAndVariableHistory() {
        List<SavingsPlanMonthDto> months = new ArrayList<>();
        for (int month = 0; month < 6; month++) {
            months.add(new SavingsPlanMonthDto(
                    YearMonth.of(2026, 5).plusMonths(month).atDay(1),
                    BigDecimal.valueOf((month + 1) * 1000L),
                    Map.of(ExpenseCategory.FOOD, new BigDecimal("2000"))));
        }

        SavingsPlanReport stable = calculate(months, RevenueType.STABLE);
        SavingsPlanReport variable = calculate(months, RevenueType.VARIABLE);

        assertThat(stable.monthlyRevenue()).isEqualByComparingTo("3500.00");
        assertThat(variable.monthlyRevenue()).isEqualByComparingTo("2250.00");
    }

    @Test
    void shouldApplyEachSavingsStrategy() {
        SavingsPlanReport safe = calculate("12000", "2000", "6000", "4800", RevenueType.STABLE, SavingsStrategy.SAFE);
        SavingsPlanReport balanced = calculate("12000", "2000", "6000", "4800", RevenueType.STABLE, SavingsStrategy.BALANCED);
        SavingsPlanReport aggressive = calculate("12000", "2000", "6000", "4800", RevenueType.STABLE, SavingsStrategy.AGGRESSIVE);

        assertThat(safe.availablePerMonth()).isEqualByComparingTo("720.00");
        assertThat(balanced.availablePerMonth()).isEqualByComparingTo("960.00");
        assertThat(aggressive.availablePerMonth()).isEqualByComparingTo("1200.00");
    }

    @Test
    void shouldScaleMonthlyNeedToDaysWhenTargetIsLessThanOneMonthAway() {
        SavingsPlanReport report = calculate("2124", "2000", "6000", "4800",
                RevenueType.STABLE, SavingsStrategy.BALANCED, TODAY.plusDays(16));

        assertThat(report.neededPerMonth()).isEqualByComparingTo("232.50");
        assertThat(report.result()).isEqualTo(GoalResult.CAN_REACH_GOAL);
    }

    @Test
    void shouldCalculateMonthlyNeedWithoutDividingByZeroForTomorrowTarget() {
        SavingsPlanReport report = calculate("2124", "2000", "6000", "4800",
                RevenueType.STABLE, SavingsStrategy.BALANCED, TODAY.plusDays(1));

        assertThat(report.neededPerMonth()).isEqualByComparingTo("3720.00");
        assertThat(report.result()).isEqualTo(GoalResult.CANNOT_REACH_GOAL);
    }

    @Test
    void shouldUseDayBasedNeedForTargetsUpToSixtyDaysAway() {
        SavingsPlanReport report = calculate("2124", "2000", "6000", "4800",
                RevenueType.STABLE, SavingsStrategy.BALANCED, TODAY.plusDays(45));

        assertThat(report.neededPerMonth()).isEqualByComparingTo("82.67");
    }

    @Test
    void shouldUseMonthBasedNeedForTargetsMoreThanSixtyDaysAway() {
        SavingsPlanReport report = calculate("2124", "2000", "6000", "4800",
                RevenueType.STABLE, SavingsStrategy.BALANCED, TODAY.plusDays(61));

        assertThat(report.neededPerMonth()).isEqualByComparingTo("62.00");
    }

    private SavingsPlanReport calculate(String target, String savings, String revenue, String expenses,
                                        RevenueType revenueType, SavingsStrategy strategy) {
        return calculate(target, savings, revenue, expenses, revenueType, strategy, TODAY.plusMonths(8));
    }

    private SavingsPlanReport calculate(String target, String savings, String revenue, String expenses,
                                        RevenueType revenueType, SavingsStrategy strategy, LocalDate targetDate) {
        SavingsPlanRequest request = new SavingsPlanRequest(
                new BigDecimal(target), targetDate, new BigDecimal(savings), revenueType, strategy);
        List<SavingsPlanMonthDto> monthlyData = new ArrayList<>();
        for (int month = 0; month < 6; month++) {
            monthlyData.add(new SavingsPlanMonthDto(
                    YearMonth.of(2026, 5).plusMonths(month).atDay(1),
                    new BigDecimal(revenue),
                    Map.of(ExpenseCategory.FOOD, new BigDecimal(expenses))));
        }
        return calculator.calculate(request, monthlyData, TODAY);
    }

    private SavingsPlanReport calculate(List<SavingsPlanMonthDto> monthlyData, RevenueType revenueType) {
        SavingsPlanRequest request = new SavingsPlanRequest(
                new BigDecimal("12000"),
                TODAY.plusMonths(8),
                new BigDecimal("2000"),
                revenueType,
                SavingsStrategy.BALANCED);
        return calculator.calculate(request, monthlyData, TODAY);
    }
}
