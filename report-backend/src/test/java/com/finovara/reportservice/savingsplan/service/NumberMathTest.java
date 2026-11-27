package com.finovara.reportservice.savingsplan.service;

import com.finovara.reportservice.savingsplan.service.money.NumberMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NumberMathTest {

    private final NumberMath numberMath = new NumberMath();

    @Test
    void shouldFindTypicalAmountForEvenList() {
        assertThat(numberMath.calculateTypicalAmount(values("1", "3"))).isEqualByComparingTo("2.00");
    }

    @Test
    void shouldFindTypicalAmountForOddList() {
        assertThat(numberMath.calculateTypicalAmount(values("1", "2", "3"))).isEqualByComparingTo("2.00");
    }

    @Test
    void shouldReturnZeroForEmptyList() {
        assertThat(numberMath.calculateTypicalAmount(List.of())).isEqualByComparingTo("0.00");
        assertThat(numberMath.calculateAverageAmount(List.of())).isEqualByComparingTo("0.00");
        assertThat(numberMath.calculateLowRevenue(List.of())).isEqualByComparingTo("0.00");
        assertThat(numberMath.calculateChangeRate(List.of())).isEqualByComparingTo("0");
    }

    @Test
    void shouldKeepSingleValueWhenThereIsOnlyOne() {
        assertThat(numberMath.calculateTypicalAmount(values("4.25"))).isEqualByComparingTo("4.25");
        assertThat(numberMath.calculateAverageAmount(values("4.25"))).isEqualByComparingTo("4.25");
        assertThat(numberMath.calculateLowRevenue(values("4.25"))).isEqualByComparingTo("4.25");
    }

    @Test
    void shouldAverageAllValues() {
        assertThat(numberMath.calculateAverageAmount(values("1", "2", "3"))).isEqualByComparingTo("2.00");
    }

    @Test
    void shouldAverageSingleValue() {
        assertThat(numberMath.calculateAverageAmount(values("4.25"))).isEqualByComparingTo("4.25");
    }

    @Test
    void shouldFindLowRevenueForVariableMonths() {
        assertThat(numberMath.calculateLowRevenue(values("1000", "2000", "3000"))).isEqualByComparingTo("1500.00");
    }

    private List<BigDecimal> values(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }
}
