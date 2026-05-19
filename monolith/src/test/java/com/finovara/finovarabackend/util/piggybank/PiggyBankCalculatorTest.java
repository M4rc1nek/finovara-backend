package com.finovara.finovarabackend.util.piggybank;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class PiggyBankCalculatorTest {

    @Test
    void shouldReturnZeroWhenGoalAmountIsNull() {
        PiggyBank piggyBank = mock(PiggyBank.class);
        when(piggyBank.getGoalAmount()).thenReturn(null);

        Double result = PiggyBankCalculator.calculateProgress(piggyBank);

        assertEquals(0.0, result);
    }

    @Test
    void shouldReturnZeroWhenGoalAmountIsZero() {
        PiggyBank piggyBank = mock(PiggyBank.class);
        when(piggyBank.getGoalAmount()).thenReturn(BigDecimal.ZERO);

        Double result = PiggyBankCalculator.calculateProgress(piggyBank);

        assertEquals(0.0, result);
    }

    @Test
    void shouldReturnZeroWhenGoalAmountIsNegative() {
        PiggyBank piggyBank = mock(PiggyBank.class);
        when(piggyBank.getGoalAmount()).thenReturn(new BigDecimal("-100"));

        Double result = PiggyBankCalculator.calculateProgress(piggyBank);

        assertEquals(0.0, result);
    }

    @Test
    void shouldCalculateProgressCorrectly() {
        PiggyBank piggyBank = mock(PiggyBank.class);
        when(piggyBank.getAmount()).thenReturn(new BigDecimal("50"));
        when(piggyBank.getGoalAmount()).thenReturn(new BigDecimal("100"));

        Double result = PiggyBankCalculator.calculateProgress(piggyBank);

        assertEquals(0.5, result);
    }

    @Test
    void shouldRoundToFourDecimalPlaces() {
        PiggyBank piggyBank = mock(PiggyBank.class);
        when(piggyBank.getAmount()).thenReturn(new BigDecimal("1"));
        when(piggyBank.getGoalAmount()).thenReturn(new BigDecimal("3"));

        Double result = PiggyBankCalculator.calculateProgress(piggyBank);

        assertEquals(0.3333, result);
    }

    @Test
    void shouldHandleAmountGreaterThanGoal() {
        PiggyBank piggyBank = mock(PiggyBank.class);
        when(piggyBank.getAmount()).thenReturn(new BigDecimal("150"));
        when(piggyBank.getGoalAmount()).thenReturn(new BigDecimal("100"));

        Double result = PiggyBankCalculator.calculateProgress(piggyBank);

        assertEquals(1.5, result);
    }
}
