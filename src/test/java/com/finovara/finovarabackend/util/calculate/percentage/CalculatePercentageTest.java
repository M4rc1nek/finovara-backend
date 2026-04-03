package com.finovara.finovarabackend.util.calculate.percentage;

import com.finovara.finovarabackend.util.service.calculate.percentage.CalculatePercentage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatePercentageTest {

    @Test
    void shouldReturnCorrectPercentage() {
        BigDecimal part = BigDecimal.valueOf(50);
        BigDecimal total = BigDecimal.valueOf(200);

        BigDecimal result = CalculatePercentage.calculatePercentage(part, total);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(25.00));
    }

    @Test
    void shouldReturnZeroWhenPartIsNull() {
        BigDecimal total = BigDecimal.valueOf(200);

        BigDecimal result = CalculatePercentage.calculatePercentage(null, total);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZeroWhenTotalIsNull() {
        BigDecimal part = BigDecimal.valueOf(50);

        BigDecimal result = CalculatePercentage.calculatePercentage(part, null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZeroWhenTotalIsZero() {
        BigDecimal part = BigDecimal.valueOf(50);
        BigDecimal total = BigDecimal.ZERO;

        BigDecimal result = CalculatePercentage.calculatePercentage(part, total);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);

    }
}


