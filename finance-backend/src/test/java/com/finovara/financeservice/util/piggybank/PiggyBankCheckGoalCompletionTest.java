package com.finovara.financeservice.util.piggybank;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiggyBankCheckGoalCompletionTest {

    private PiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("150.00"));
    }

    @Test
    void shouldReturnFalseWhenGoalAmountIsNull() {
        piggyBank.setGoalAmount(null);

        boolean result = PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank);

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenAmountIsLessThanGoalAmount() {
        piggyBank.setGoalAmount(new BigDecimal("200.00"));
        boolean result = PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank);

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenAmountEqualsGoalAmount() {
        piggyBank.setGoalAmount(new BigDecimal("150.00"));
        boolean result = PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank);

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenAmountIsGreaterThanGoalAmount() {
        piggyBank.setGoalAmount(new BigDecimal("100.00"));
        boolean result = PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank);

        assertTrue(result);
    }
}