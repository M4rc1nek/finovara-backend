package com.finovara.reportservice.savingsplan.service.money;

import com.finovara.reportservice.savingsplan.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

@Component
public class NumberMath {

    private static final BigDecimal LOW_REVENUE_PART = new BigDecimal("0.25");
    private static final MathContext MATH = MathContext.DECIMAL128;

    public BigDecimal calculateTypicalAmount(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return MoneyUtils.ZERO_MONEY;
        }
        List<BigDecimal> sortedValues = values.stream().sorted().toList();
        int middleIndex = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) {
            return MoneyUtils.round(sortedValues.get(middleIndex));
        }
        return MoneyUtils.round(sortedValues.get(middleIndex - 1)
                .add(sortedValues.get(middleIndex))
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP));
    }

    public BigDecimal calculateLowRevenue(List<BigDecimal> monthlyRevenues) {
        if (monthlyRevenues.isEmpty()) {
            return MoneyUtils.ZERO_MONEY;
        }
        List<BigDecimal> sortedMonthlyRevenues = monthlyRevenues.stream().sorted().toList();
        BigDecimal lowRevenueMonthIndex = BigDecimal.valueOf(sortedMonthlyRevenues.size() - 1L)
                .multiply(LOW_REVENUE_PART);
        int firstMonthIndex = lowRevenueMonthIndex.intValue();
        BigDecimal lowMonthlyRevenue = getLowMonthlyRevenue(firstMonthIndex, sortedMonthlyRevenues, lowRevenueMonthIndex);
        return MoneyUtils.round(lowMonthlyRevenue);
    }

    private BigDecimal getLowMonthlyRevenue(int firstMonthIndex, List<BigDecimal> sortedMonthlyRevenues, BigDecimal lowRevenueMonthIndex) {
        int secondMonthIndex = Math.min(firstMonthIndex + 1, sortedMonthlyRevenues.size() - 1);
        BigDecimal partFromSecondMonth = lowRevenueMonthIndex.subtract(BigDecimal.valueOf(firstMonthIndex));
        BigDecimal firstMonthRevenue = sortedMonthlyRevenues.get(firstMonthIndex);
        BigDecimal secondMonthRevenue = sortedMonthlyRevenues.get(secondMonthIndex);
        return firstMonthRevenue
                .multiply(BigDecimal.ONE.subtract(partFromSecondMonth))
                .add(secondMonthRevenue.multiply(partFromSecondMonth));
    }

    public BigDecimal calculateAverageAmount(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return MoneyUtils.ZERO_MONEY;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return MoneyUtils.round(sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP));
    }

    public BigDecimal calculateChangeRate(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal averageAmount = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MATH);
        if (averageAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalSquaredDifference = values.stream()
                .map(value -> value.subtract(averageAmount).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageSquaredDifference = totalSquaredDifference.divide(BigDecimal.valueOf(values.size()), MATH);
        return averageSquaredDifference.sqrt(MATH).divide(averageAmount, MATH);
    }
}
