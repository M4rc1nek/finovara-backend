package com.finovara.finovarabackend.util.calculate.percentage;

import com.finovara.finovarabackend.util.percentage.CalculatePercentage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculateValueFromPercentageTest {

    @Test
    void shouldReturnCorrectValueFromPercentage() {
        BigDecimal amount = BigDecimal.valueOf(200);
        BigDecimal percentage = BigDecimal.valueOf(25);

        BigDecimal result = CalculatePercentage.calculateValueFromPercentage(amount, percentage);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void shouldReturnZeroWhenAmountIsNull() {
        BigDecimal percentage = BigDecimal.valueOf(25);

        BigDecimal result = CalculatePercentage.calculateValueFromPercentage(null, percentage);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZeroWhenPercentageIsNull() {
        BigDecimal amount = BigDecimal.valueOf(200);

        BigDecimal result = CalculatePercentage.calculateValueFromPercentage(amount, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZeroWhenPercentageIsZero() {
        BigDecimal amount = BigDecimal.valueOf(200);
        BigDecimal percentage = BigDecimal.ZERO;

        BigDecimal result = CalculatePercentage.calculateValueFromPercentage(amount, percentage);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnCorrectValueWhenPercentageIsGreaterThan100() {
        BigDecimal amount = BigDecimal.valueOf(200);
        BigDecimal percentage = BigDecimal.valueOf(150);

        BigDecimal result = CalculatePercentage.calculateValueFromPercentage(amount, percentage);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(300.00));
    }

    @Test
    void shouldReturnZeroWhenAmountAndPercentageAreNull() {
        BigDecimal result = CalculatePercentage.calculateValueFromPercentage(null, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
