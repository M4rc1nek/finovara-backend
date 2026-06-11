package com.finovara.corebackend.util.percentage;

import com.finovara.contracts.percentage.CalculatePercentage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatePercentageTest {

    @Nested
    class CalculatePercentageTests {
        @Test
        void shouldReturnCorrectPercentage() {
            BigDecimal result = CalculatePercentage.calculatePercentage(BigDecimal.valueOf(50), BigDecimal.valueOf(200));

            assertThat(result).isEqualByComparingTo("25.00");
        }

        @Test
        void shouldReturnZeroWhenPartIsNull() {
            BigDecimal result = CalculatePercentage.calculatePercentage(null, BigDecimal.valueOf(200));

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenTotalIsNull() {
            BigDecimal result = CalculatePercentage.calculatePercentage(BigDecimal.valueOf(50), null);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenTotalIsZero() {
            BigDecimal result = CalculatePercentage.calculatePercentage(BigDecimal.valueOf(50), BigDecimal.ZERO);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class CalculateValueFromPercentageTests {
        @Test
        void shouldReturnCorrectValueFromPercentage() {
            BigDecimal result = CalculatePercentage.calculateValueFromPercentage(BigDecimal.valueOf(200), BigDecimal.valueOf(25));

            assertThat(result).isEqualByComparingTo("50.00");
        }

        @Test
        void shouldReturnZeroWhenAmountIsNull() {
            BigDecimal result = CalculatePercentage.calculateValueFromPercentage(null, BigDecimal.valueOf(25));

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenPercentageIsNull() {
            BigDecimal result = CalculatePercentage.calculateValueFromPercentage(BigDecimal.valueOf(200), null);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnZeroWhenPercentageIsZero() {
            BigDecimal result = CalculatePercentage.calculateValueFromPercentage(BigDecimal.valueOf(200), BigDecimal.ZERO);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnCorrectValueWhenPercentageIsGreaterThan100() {
            BigDecimal result = CalculatePercentage.calculateValueFromPercentage(BigDecimal.valueOf(200), BigDecimal.valueOf(150));

            assertThat(result).isEqualByComparingTo("300.00");
        }

        @Test
        void shouldReturnZeroWhenBothAreNull() {
            BigDecimal result = CalculatePercentage.calculateValueFromPercentage(null, null);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}